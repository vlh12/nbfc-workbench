package com.nbfc.accessdata.access;
import com.nbfc.accessdata.data.portfolio.*;
import com.nbfc.shared.PortfolioData.*;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.http.*;
import org.springframework.web.server.ResponseStatusException;
import java.io.IOException;
import java.util.*;

@io.swagger.v3.oas.annotations.tags.Tag(name="Portfolios and supporting data",description="Portfolio CSV/JSON uploads, capital, NBFC asset quality and threshold versions.")
@RestController @RequestMapping("/api/portfolios")
public class PortfolioController {
    private final PortfolioCsv csv; private final PortfolioStore store;
    public PortfolioController(PortfolioCsv csv,PortfolioStore store){this.csv=csv;this.store=store;}
    @PostMapping("/validate") @PreAuthorize("hasRole('CONTRIBUTOR')")
    public Validation validate(@RequestParam MultipartFile file,@RequestParam boolean synthetic,Authentication actor)throws IOException{
        checkSynthetic(synthetic); var result=csv.validate(file.getBytes());
        if(!result.errors().isEmpty())store.rejected(result,actor.getName());
        return result;
    }
    @PostMapping(consumes=MediaType.MULTIPART_FORM_DATA_VALUE) @PreAuthorize("hasRole('CONTRIBUTOR')")
    public ResponseEntity<?> save(@RequestParam MultipartFile file,@RequestParam boolean synthetic,@RequestParam(required=false) String name,Authentication actor)throws IOException{
        checkSynthetic(synthetic); var validation=csv.validate(file.getBytes());
        if(!validation.errors().isEmpty()){
            var rejected=store.rejected(validation,actor.getName());
            return ResponseEntity.unprocessableEntity().body(Map.of("message","No loan rows saved. Correct the listed errors.","submissionId",rejected.id(),"issues",validation.errors()));
        }
        return ResponseEntity.status(201).body(store.save(validation,actor.getName(),name));
    }
    public record JsonSubmission(List<Loan> rows,boolean synthetic,String name){
        public JsonSubmission(List<Loan> rows,boolean synthetic){this(rows,synthetic,null);}
    }
    @PostMapping(consumes=MediaType.APPLICATION_JSON_VALUE) @PreAuthorize("hasRole('CONTRIBUTOR')")
    public ResponseEntity<?> json(@RequestBody JsonSubmission body,Authentication actor){
        checkSynthetic(body.synthetic());
        if(body.rows()==null||body.rows().isEmpty()||body.rows().size()>10000)
            throw new IllegalArgumentException("Provide 1 to 10000 portfolio loan rows.");
        if(body.rows().stream().anyMatch(java.util.Objects::isNull))throw new IllegalArgumentException("Loan row must not be null.");
        boolean explicit=body.rows().stream().anyMatch(l->l.eadAmount()!=null);
        StringBuilder text=new StringBuilder(PortfolioCsv.HEADER+(explicit?",ead_amount":"")).append("\n");
        for(Loan l:body.rows()){
            if(l==null)throw new IllegalArgumentException("Loan row must not be null.");
            Object[] values={l.bankId(),l.nbfcId(),l.loanId(),l.reportingDate(),l.instrumentType(),l.amountLent(),l.interestAmount(),l.totalPayable(),l.amountRepaid(),l.outstandingAmount(),l.dpdDays(),l.loanStatus(),l.maturityDate(),l.collateralType(),l.revenuePeriodStart(),l.revenuePeriodEnd(),l.revenueAmount(),l.currency(),l.unit()};
            var row=new java.util.ArrayList<String>();
            for(Object value:values){String v=value==null?"":value.toString();if(v.contains(",")||v.contains("\n")||v.contains("\r"))throw new IllegalArgumentException("JSON field contains a delimiter.");row.add(v);}
            if(explicit){String v=l.eadAmount()==null?"":l.eadAmount();if(v.contains(",")||v.contains("\n")||v.contains("\r"))throw new IllegalArgumentException("Invalid EAD value.");row.add(v);}
            text.append(String.join(",",row)).append("\n");
        }
        var validation=csv.validate(text.toString().getBytes(java.nio.charset.StandardCharsets.UTF_8));
        if(!validation.errors().isEmpty()){
            var rejected=store.rejected(validation,actor.getName());
            return ResponseEntity.unprocessableEntity().body(Map.of("message","No loan rows saved.","submissionId",rejected.id(),"issues",validation.errors()));
        }
        return ResponseEntity.status(201).body(store.save(validation,actor.getName(),body.name()));
    }
    @GetMapping @PreAuthorize("hasAnyRole('CONTRIBUTOR','ANALYST','SUPERVISOR')")
    public List<Entry> list(Authentication actor){return store.list().stream().filter(row->!analystOnly(actor)||"APPROVED".equals(row.status())).toList();}
    @GetMapping("/{id}") @PreAuthorize("hasAnyRole('CONTRIBUTOR','ANALYST','SUPERVISOR')")
    public Snapshot snapshot(@PathVariable String id,Authentication actor){if(analystOnly(actor))store.requireApproved(id);return store.get(id,"PORTFOLIO",Snapshot.class);}
    @PostMapping("/capital") @PreAuthorize("hasRole('CONTRIBUTOR')")
    public Entry capital(@RequestBody Capital body,Authentication actor){return store.capital(body,actor.getName());}
    @GetMapping("/capital/{id}") @PreAuthorize("hasAnyRole('CONTRIBUTOR','ANALYST','SUPERVISOR')")
    public Capital capital(@PathVariable String id,Authentication actor){if(analystOnly(actor))store.requireApproved(id);return store.get(id,"CAPITAL",Capital.class);}
    @PostMapping("/quality") @PreAuthorize("hasRole('CONTRIBUTOR')")
    public Entry quality(@RequestBody Quality body,Authentication actor){return store.quality(body,actor.getName());}
    @GetMapping("/quality/{id}") @PreAuthorize("hasAnyRole('CONTRIBUTOR','ANALYST','SUPERVISOR')")
    public Quality quality(@PathVariable String id,Authentication actor){if(analystOnly(actor))store.requireApproved(id);return store.get(id,"QUALITY",Quality.class);}
    @PostMapping("/thresholds") @PreAuthorize("hasRole('SUPERVISOR')")
    public Entry thresholds(@RequestBody Thresholds body,Authentication actor){return store.thresholds(body,actor.getName());}
    @GetMapping("/thresholds/{id}") @PreAuthorize("hasAnyRole('CONTRIBUTOR','ANALYST','SUPERVISOR')")
    public Thresholds thresholds(@PathVariable String id,Authentication actor){if(analystOnly(actor))store.requireApproved(id);return store.get(id,"THRESHOLDS",Thresholds.class);}
    private boolean analystOnly(Authentication actor){return actor!=null&&actor.getAuthorities().stream().anyMatch(a->a.getAuthority().equals("ROLE_ANALYST"))&&actor.getAuthorities().stream().noneMatch(a->a.getAuthority().equals("ROLE_SUPERVISOR"));}
    public record DataDecision(String decision,String note){}
    @PostMapping("/{id}/review") @PreAuthorize("hasRole('SUPERVISOR')")
    public Entry review(@PathVariable String id,@RequestBody DataDecision body,Authentication actor){return store.review(id,body.decision(),body.note(),actor.getName());}
    @GetMapping("/{id}/reviews") @PreAuthorize("hasAnyRole('CONTRIBUTOR','SUPERVISOR')")
    public List<DataReview> reviews(@PathVariable String id){return store.reviewHistory(id);}
    @ExceptionHandler(org.springframework.dao.OptimisticLockingFailureException.class)
    public ResponseEntity<?> conflict(){return ResponseEntity.status(409).body(Map.of("message","Another Supervisor has already reviewed this record. Refresh the queue."));}
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<?> invalid(IllegalArgumentException e){return ResponseEntity.unprocessableEntity().body(Map.of("message",e.getMessage()));}
    private void checkSynthetic(boolean value){if(!value)throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Confirm synthetic data only.");}
}
