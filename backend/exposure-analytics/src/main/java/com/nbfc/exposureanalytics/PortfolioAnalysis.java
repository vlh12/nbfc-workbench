package com.nbfc.exposureanalytics;
import com.nbfc.shared.PortfolioData.*;
import com.nbfc.shared.LocalServiceClient;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.beans.factory.annotation.Value;
import java.math.*;
import java.util.*;

@io.swagger.v3.oas.annotations.tags.Tag(name="Portfolio analytics",description="Explicit EAD, concentration and reported amounts in the selected portfolio.")
@RestController @RequestMapping("/api/exposures/portfolios")
public class PortfolioAnalysis {
    private final String access;
    public PortfolioAnalysis(@Value("${ACCESS_DATA_URL:http://localhost:8081}") String access){this.access=access;}
    @GetMapping("/{id}") @PreAuthorize("hasAnyRole('ANALYST','SUPERVISOR')")
    public Analysis analyze(@PathVariable String id){
        return calculate(LocalServiceClient.get(access,"/api/portfolios/"+id,Snapshot.class));
    }
    public static Analysis calculate(Snapshot s){
        boolean explicit=s.rows().stream().allMatch(l->l.eadAmount()!=null);
        BigDecimal totalEad=explicit?s.rows().stream().map(l->new BigDecimal(l.eadAmount())).reduce(BigDecimal.ZERO,BigDecimal::add):null;
        var grouped=new TreeMap<String,List<Loan>>();
        for(var l:s.rows())grouped.computeIfAbsent(l.nbfcId(),k->new ArrayList<>()).add(l);
        var totals=new ArrayList<Nbfc>();
        for(var e:grouped.entrySet()){
            var rows=e.getValue(); BigDecimal lent=BigDecimal.ZERO,outstanding=BigDecimal.ZERO,ead=BigDecimal.ZERO;
            var status=new TreeMap<String,BigDecimal>();
            for(var l:rows){
                lent=lent.add(new BigDecimal(l.amountLent()));outstanding=outstanding.add(new BigDecimal(l.outstandingAmount()));
                if(explicit)ead=ead.add(new BigDecimal(l.eadAmount()));
                status.merge(l.loanStatus(),new BigDecimal(l.outstandingAmount()),BigDecimal::add);
            }
            var amounts=new TreeMap<String,String>();status.forEach((k,v)->amounts.put(k,v.toPlainString()));
            String share=totalEad!=null&&totalEad.signum()>0?ead.multiply(BigDecimal.valueOf(100)).divide(totalEad,6,RoundingMode.HALF_UP).toPlainString():null;
            totals.add(new Nbfc(e.getKey(),rows.size(),lent.toPlainString(),outstanding.toPlainString(),explicit?ead.toPlainString():null,share,amounts));
        }
        String lent=s.rows().stream().map(l->new BigDecimal(l.amountLent())).reduce(BigDecimal.ZERO,BigDecimal::add).toPlainString();
        String outstanding=s.rows().stream().map(l->new BigDecimal(l.outstandingAmount())).reduce(BigDecimal.ZERO,BigDecimal::add).toPlainString();
        return new Analysis(s.id(),s.bankId(),s.reportingDate(),s.status(),s.rows().size(),lent,outstanding,totalEad==null?null:totalEad.toPlainString(),"Total explicit EAD in this selected snapshot (INR rupees); not a regulatory limit",totals,s.warnings());
    }
}
