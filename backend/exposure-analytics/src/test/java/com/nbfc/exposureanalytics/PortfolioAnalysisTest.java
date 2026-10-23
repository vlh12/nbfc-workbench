package com.nbfc.exposureanalytics;
import com.nbfc.shared.PortfolioData.*;
import org.junit.jupiter.api.Test;
import java.time.*;
import java.util.*;
import static org.assertj.core.api.Assertions.*;
class PortfolioAnalysisTest {
 private Loan loan(String id,String nbfc,String ead){return new Loan("SYN-BANK-001",nbfc,id,LocalDate.of(2026,9,30),"TERM_LOAN","100","10","110","10","100",0,"CURRENT",LocalDate.of(2027,9,30),"SECURED",LocalDate.of(2026,7,1),LocalDate.of(2026,9,30),"7","INR","RUPEE",ead);}
 @Test void aggregationUsesExplicitEadAndLabelsDenominator(){
  var s=new Snapshot("s","SYN-BANK-001",LocalDate.of(2026,9,30),"APPROVED","actor",Instant.now(),List.of(loan("a","SYN-NBFC-001","30"),loan("b","SYN-NBFC-002","70")),List.of());
  var a=PortfolioAnalysis.calculate(s);
  assertThat(a.totalLent()).isEqualTo("200");assertThat(a.totalOutstanding()).isEqualTo("200");assertThat(a.totalEad()).isEqualTo("100");
  assertThat(a.nbfcs().getFirst().portfolioSharePct()).isEqualTo("30.000000");
  assertThat(a.concentrationDenominator()).contains("explicit EAD");
 }
 @Test void missingEadIsNeverSubstitutedWithLendingOrOutstanding(){
  var s=new Snapshot("s","SYN-BANK-001",LocalDate.of(2026,9,30),"VALIDATED","actor",Instant.now(),List.of(loan("a","SYN-NBFC-001",null)),List.of());
  var a=PortfolioAnalysis.calculate(s);assertThat(a.totalEad()).isNull();assertThat(a.nbfcs().getFirst().portfolioSharePct()).isNull();
 }
}
