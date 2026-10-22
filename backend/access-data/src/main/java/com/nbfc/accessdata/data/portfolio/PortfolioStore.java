package com.nbfc.accessdata.data.portfolio;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nbfc.shared.PortfolioData;
import com.nbfc.shared.PortfolioData.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import java.time.*;
import java.util.*;

@Service
@Transactional(readOnly=true)
public class PortfolioStore {
    private final PortfolioDocuments documents; private final ObjectMapper json;
    public PortfolioStore(PortfolioDocuments documents,ObjectMapper json){this.documents=documents;this.json=json;}
    private DataReviews reviews;
    @org.springframework.beans.factory.annotation.Autowired
    public void setReviews(DataReviews reviews){this.reviews=reviews;}
    @Transactional
    public Snapshot save(Validation result,String actor){ return save(result,actor,null); }
    @Transactional
    public Snapshot save(Validation result,String actor,String name){
        if(name!=null) {
            require(name.chars().noneMatch(Character::isISOControl),"Portfolio name cannot contain control characters.");
            name=name.strip();
            require(!name.isBlank()&&name.length()<=120,"Provide a portfolio name from 1 to 120 characters.");
        }
        if(!result.errors().isEmpty()) throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY,"Correct the validation errors.");
        var first=result.rows().getFirst();
        String status="PENDING_REVIEW";
        String id=UUID.randomUUID().toString(); Instant now=Instant.now();
        var snapshot=new Snapshot(id,first.bankId(),first.reportingDate(),status,actor,now,result.rows(),result.warnings(),name);
        put(id,"PORTFOLIO",snapshot.bankId(),snapshot.reportingDate(),status,actor,result.rows().size(),now,snapshot);
        return snapshot;
    }
    @Transactional
    public Entry rejected(Validation result,String actor){return put(UUID.randomUUID().toString(),"PORTFOLIO",PortfolioData.BANK,null,"VALIDATION_FAILED",actor,0,Instant.now(),result);}
    @Transactional
    public Entry capital(Capital c,String actor){
        require(c!=null && PortfolioData.BANK.equals(c.bankId())&&c.reportingDate()!=null,"Provide the demo bank and reporting date.");
        require(PortfolioCsv.money(c.eligibleCapital()).signum()>0,"Eligible capital must be positive.");
        require(c.label()!=null&&!c.label().isBlank()&&c.label().length()<=200,"Provide a capital denominator label.");
        return put(UUID.randomUUID().toString(),"CAPITAL",c.bankId(),c.reportingDate(),"PENDING_REVIEW",actor,1,Instant.now(),c);
    }
    @Transactional
    public Entry quality(Quality q,String actor){
        require(q!=null && PortfolioData.BANK.equals(q.bankId())&&q.reportingDate()!=null,"Provide the demo bank and reporting date.");
        require(q.nbfcId()!=null && q.nbfcId().matches("SYN-NBFC-[A-Z0-9-]{1,35}")&&!q.nbfcId().equals(q.bankId()),"Provide a synthetic NBFC counterparty.");
        Set<String> keys=Set.of("CURRENT","DPD_1_30","DPD_31_60","DPD_61_90","NPA_90_PLUS","RESTRUCTURED","WRITE_OFF_RECOVERY");
        require(q.buckets()!=null&&q.buckets().keySet().equals(keys),"Provide all seven asset-quality buckets with no extra keys.");
        var total=q.buckets().values().stream().map(PortfolioCsv::money).reduce(java.math.BigDecimal.ZERO,java.math.BigDecimal::add);
        require(total.compareTo(PortfolioCsv.money(q.totalAmount()))==0,"Bucket amounts must equal totalAmount. Submit mutually exclusive synthetic aggregates.");
        require(total.signum()>0,"Asset-quality total must be positive.");
        return put(UUID.randomUUID().toString(),"QUALITY",q.bankId(),q.reportingDate(),"PENDING_REVIEW",actor,7,Instant.now(),q);
    }
    @Transactional
    public Entry thresholds(Thresholds t,String actor){
        require(t!=null&&PortfolioData.BANK.equals(t.bankId())&&t.name()!=null&&!t.name().isBlank()&&t.name().length()<=120,"Provide demo bank and threshold version name.");
        var medium=PortfolioCsv.money(t.mediumPct()); var high=PortfolioCsv.money(t.highPct());
        require(medium.signum()>0&&high.compareTo(medium)>0,"Require 0 < medium threshold < high threshold.");
        return put(UUID.randomUUID().toString(),"THRESHOLDS",t.bankId(),null,"APPROVED",actor,1,Instant.now(),t);
    }
    public List<Entry> list(){return documents.findTop100ByBankIdOrderByCreatedAtDescIdDesc(PortfolioData.BANK).stream().map(this::entry).toList();}
    public <T> T get(String id,String kind,Class<T> type){
        var d=documents.findByIdAndBankId(id,PortfolioData.BANK).orElseThrow(()->new ResponseStatusException(HttpStatus.NOT_FOUND,"Record not found."));
        if(!kind.equals(d.kind)||"VALIDATION_FAILED".equals(d.status)) throw new ResponseStatusException(HttpStatus.NOT_FOUND,"Record not found.");
        try{
            if(type==Snapshot.class){var s=json.readValue(d.payload,Snapshot.class);return type.cast(new Snapshot(s.id(),s.bankId(),s.reportingDate(),d.status,s.submittedBy(),s.createdAt(),s.rows(),s.warnings(),s.name()));}
            return json.readValue(d.payload,type);}catch(JsonProcessingException e){throw new IllegalStateException("Stored portfolio could not be read.",e);}
    }
    public void requireApproved(String id){
        var d=documents.findByIdAndBankId(id,PortfolioData.BANK).orElseThrow(()->new ResponseStatusException(HttpStatus.NOT_FOUND,"Record not found."));
        if(!"APPROVED".equals(d.status))throw new ResponseStatusException(HttpStatus.FORBIDDEN,"Supervisor data approval is required before analyst access.");
    }
    public List<DataReview> reviewHistory(String id){getDocument(id);return reviews.findByDocumentIdOrderByReviewedAtAscIdAsc(id);}
    private PortfolioDocument getDocument(String id){return documents.findByIdAndBankId(id,PortfolioData.BANK).orElseThrow(()->new ResponseStatusException(HttpStatus.NOT_FOUND,"Record not found."));}
    @Transactional
    public Entry review(String id,String decision,String note,String actor){
        var d=getDocument(id);
        require(Set.of("PORTFOLIO","CAPITAL","QUALITY").contains(d.kind),"Only contributor datasets require this review.");
        if(!"PENDING_REVIEW".equals(d.status))throw new ResponseStatusException(HttpStatus.CONFLICT,"This data has already been reviewed. Corrections require a new version.");
        if(actor.equals(d.submittedBy))throw new ResponseStatusException(HttpStatus.FORBIDDEN,"You cannot review your own submitted data.");
        require(Set.of("APPROVED","RETURNED").contains(decision),"Choose APPROVED or RETURNED.");
        require(note!=null&&!note.isBlank()&&note.length()<=2000,"Provide a review reason up to 2000 characters.");
        if("APPROVED".equals(decision)&&"PORTFOLIO".equals(d.kind)){
            var s=get(id,"PORTFOLIO",Snapshot.class);
            require(s.warnings().isEmpty()&&s.rows().stream().allMatch(row->row.eadAmount()!=null),"Correct reconciliation warnings and provide explicit EAD before approval. Return this version and upload a correction.");
        }
        var event=new DataReview();event.id=UUID.randomUUID().toString();event.documentId=id;event.decision=decision;event.reviewedBy=actor;event.reviewedAt=Instant.now();event.reviewNote=note.strip();
        reviews.save(event);d.status=decision;documents.saveAndFlush(d);return entry(d);
    }
    private Entry put(String id,String kind,String bank,LocalDate date,String status,String actor,int count,Instant now,Object payload){
        var d=new PortfolioDocument(); d.id=id;d.kind=kind;d.bankId=bank;d.reportingDate=date;d.status=status;d.submittedBy=actor;d.createdAt=now;d.rowCount=count;
        try{d.payload=json.writeValueAsString(payload);}catch(JsonProcessingException e){throw new IllegalStateException(e);}
        return entry(documents.save(d));
    }
    private Entry entry(PortfolioDocument d){
        String name=null;
        if("PORTFOLIO".equals(d.kind)&&!"VALIDATION_FAILED".equals(d.status)) {
            try { name=json.readTree(d.payload).path("name").asText(null); }
            catch(JsonProcessingException e){throw new IllegalStateException("Stored portfolio could not be read.",e);}
            if(name==null||name.isBlank())name=PortfolioData.defaultName(d.reportingDate);
        }
        if(name==null)try{
            var payload=json.readTree(d.payload);
            name=switch(d.kind){case "CAPITAL" -> "Bank capital - "+d.reportingDate+" - "+payload.path("label").asText();case "QUALITY" -> "Asset quality - "+payload.path("nbfcId").asText()+" - "+d.reportingDate;case "THRESHOLDS" -> payload.path("name").asText("Alert thresholds");default -> "Validation attempt - "+d.createdAt;};
        }catch(JsonProcessingException e){throw new IllegalStateException(e);}
        return new Entry(d.id,d.kind,d.bankId,d.reportingDate,d.status,d.submittedBy,d.createdAt,d.rowCount,name);
    }
    private static void require(boolean valid,String message){if(!valid)throw new IllegalArgumentException(message);}
}
