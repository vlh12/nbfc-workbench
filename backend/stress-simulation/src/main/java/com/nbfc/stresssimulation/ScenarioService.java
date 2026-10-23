package com.nbfc.stresssimulation;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.nbfc.shared.*;
import com.nbfc.shared.PortfolioData.*;
import com.nbfc.shared.StressData.*;
import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;
import java.math.*;
import java.time.Instant;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.*;

@Service
public class ScenarioService {
    private final Scenarios runs; private final ObjectMapper json; private final String access;
    public ScenarioService(Scenarios runs,ObjectMapper json,@Value("${ACCESS_DATA_URL:http://localhost:8081}") String access){
        this.runs=runs;this.json=json;this.access=access;
    }
    @Transactional
    public Run execute(Request request,String actor) {
        if(request==null||request.idempotencyKey()==null||!request.idempotencyKey().matches("[A-Za-z0-9-]{1,64}"))
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Provide an idempotency key (letters, digits or hyphens).");
        String hash=hash(encode(request)); var prior=runs.findByBankIdAndCreatedByAndRequestKey(PortfolioData.BANK,actor,request.idempotencyKey());
        if(prior.isPresent()){
            if(!prior.get().requestHash.equals(hash))throw new ResponseStatusException(HttpStatus.CONFLICT,"Idempotency key is already bound to different inputs.");
            return decode(prior.get());
        }
        String id=UUID.randomUUID().toString(); Instant now=Instant.now(); Result result=null;String failure=null;
        try {
            require(request.name()==null || (!request.name().isBlank()&&request.name().length()<=120&&request.name().chars().noneMatch(Character::isISOControl)),"Provide a scenario name up to 120 characters.");
            require(request.rationale()!=null&&!request.rationale().isBlank()&&request.rationale().length()<=2000,"Provide a scenario rationale up to 2000 characters.");
            require(validId(request.snapshotId())&&validId(request.capitalId())&&validId(request.qualityId())&&validId(request.thresholdId()),"Select saved portfolio, capital, asset-quality and threshold versions.");
            var snapshot=LocalServiceClient.get(access,"/api/portfolios/"+request.snapshotId(),Snapshot.class);
            var capital=LocalServiceClient.get(access,"/api/portfolios/capital/"+request.capitalId(),Capital.class);
            var quality=LocalServiceClient.get(access,"/api/portfolios/quality/"+request.qualityId(),Quality.class);
            var thresholds=LocalServiceClient.get(access,"/api/portfolios/thresholds/"+request.thresholdId(),Thresholds.class);
            require("APPROVED".equals(snapshot.status()),"Portfolio must have explicit EAD and no unresolved reconciliation warnings.");
            require(PortfolioData.BANK.equals(snapshot.bankId())&&snapshot.bankId().equals(capital.bankId())&&snapshot.bankId().equals(quality.bankId())&&snapshot.bankId().equals(thresholds.bankId()),"All inputs must belong to the same bank.");
            require(snapshot.reportingDate().equals(capital.reportingDate())&&snapshot.reportingDate().equals(quality.reportingDate()),"Stale reporting periods: select matching dates. No stale-data override is configured.");
            require(Objects.equals(request.nbfcId(),quality.nbfcId()),"Asset-quality context must match the selected NBFC.");
            var loans=snapshot.rows().stream().filter(l->Objects.equals(request.nbfcId(),l.nbfcId())).toList();
            require(!loans.isEmpty()&&loans.stream().allMatch(l->l.eadAmount()!=null),"Selected NBFC requires explicit EAD.");
            var ead=loans.stream().map(l->new BigDecimal(l.eadAmount())).reduce(BigDecimal.ZERO,BigDecimal::add);
            result=calculate(ead,capital,thresholds,request);
        } catch(IllegalArgumentException e){failure=e.getMessage();}
          catch (ResponseStatusException e) {
            int status = e.getStatusCode().value();

            if (status == 401 || status == 403) {
                throw e;
            }

            failure = e.getReason();
        }
        var run=new Run(id,PortfolioData.BANK,request.snapshotId(),request.nbfcId(),failure==null?"COMPLETED":"FAILED",actor,now,"phase0-direct-loss-v1",request,result,failure);
        var record=new ScenarioRecord();record.id=id;record.bankId=run.bankId();record.createdBy=actor;record.createdAt=now;record.requestKey=request.idempotencyKey();record.requestHash=hash;record.payload=encode(run);runs.saveAndFlush(record);
        return run;
    }
    public static Result calculate(BigDecimal ead,Capital c,Thresholds t,Request r){
        BigDecimal capital=decimal(c.eligibleCapital()),severity=percent(r.severityPct()),recovery=percent(r.recoveryPct());
        BigDecimal medium=decimal(t.mediumPct()),high=decimal(t.highPct());
        require(ead.signum()>0&&capital.signum()>0,"EAD and eligible capital must be positive.");
        require(medium.signum()>0&&high.compareTo(medium)>0,"Invalid threshold version.");
        var rawLoss=ead.multiply(severity).multiply(BigDecimal.valueOf(100).subtract(recovery)).movePointLeft(4);
        var impact=rawLoss.multiply(BigDecimal.valueOf(100)).divide(capital,12,RoundingMode.HALF_UP);
        var concentration=ead.multiply(BigDecimal.valueOf(100)).divide(capital,6,RoundingMode.HALF_UP);
        String alert=impact.compareTo(high)>=0?"HIGH":impact.compareTo(medium)>=0?"MEDIUM":"LOW";
        return new Result(ead.toPlainString(),capital.toPlainString(),c.label(),severity.toPlainString(),recovery.toPlainString(),
            rawLoss.setScale(2,RoundingMode.HALF_UP).toPlainString(),impact.setScale(6,RoundingMode.HALF_UP).toPlainString(),
            concentration.toPlainString(),alert,r.thresholdId(),medium.toPlainString(),high.toPlainString(),
            "Direct loss = explicit EAD × severity / 100 × (1 − recovery / 100); capital impact = unrounded direct loss / selected eligible capital × 100. Money rounded HALF_UP to paise; percentages to 6 decimals. Configured prototype thresholds, not RBI limits.");
    }
    public List<Run> list(){return runs.findTop100ByBankIdOrderByCreatedAtDesc(PortfolioData.BANK).stream().map(this::decode).toList();}
    public Run get(String id){return decode(runs.findByIdAndBankId(id,PortfolioData.BANK).orElseThrow(()->new ResponseStatusException(HttpStatus.NOT_FOUND,"Scenario not found.")));}
    private Run decode(ScenarioRecord r){try{return json.readValue(r.payload,Run.class);}catch(JsonProcessingException e){throw new IllegalStateException(e);}}
    private String encode(Object o){try{return json.writeValueAsString(o);}catch(JsonProcessingException e){throw new IllegalStateException(e);}}
    private String hash(String s){try{return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(s.getBytes(StandardCharsets.UTF_8)));}catch(Exception e){throw new IllegalStateException(e);}}
    private static boolean validId(String id){return id!=null&&id.matches("[a-f0-9-]{36}");}
    private static BigDecimal decimal(String s){require(s!=null&&s.matches("[0-9]{1,16}(\\.[0-9]{1,2})?"),"Use decimal inputs with at most two places.");return new BigDecimal(s);}
    private static BigDecimal percent(String s){var v=decimal(s);require(v.compareTo(BigDecimal.valueOf(100))<=0,"Severity and recovery must be between 0 and 100.");return v;}
    private static void require(boolean condition,String message){if(!condition)throw new IllegalArgumentException(message);}
}
