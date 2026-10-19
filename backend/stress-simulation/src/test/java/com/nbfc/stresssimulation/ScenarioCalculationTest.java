package com.nbfc.stresssimulation;
import com.nbfc.shared.PortfolioData.*;
import com.nbfc.shared.StressData.*;
import org.junit.jupiter.api.Test;
import java.math.BigDecimal;
import java.time.LocalDate;
import static org.assertj.core.api.Assertions.*;
class ScenarioCalculationTest{
 private Capital capital(String amount){return new Capital("SYN-BANK-001",LocalDate.of(2026,9,30),amount,"Synthetic eligible capital");}
 private Thresholds thresholds(){return new Thresholds("SYN-BANK-001","Test-only thresholds","5","10");}
 private Request request(String severity,String recovery){return new Request("snapshot","SYN-NBFC-001","capital","quality","threshold-v1",severity,recovery,"Synthetic scenario","key");}
 @Test void proposalWorkedExampleReturns504CroreAnd1008Percent(){
  var r=ScenarioService.calculate(new BigDecimal("12000000000"),capital("50000000000"),thresholds(),request("60","30"));
  assertThat(r.stressedLoss()).isEqualTo("5040000000.00");assertThat(r.capitalImpactPct()).isEqualTo("10.080000");
  assertThat(r.concentrationCapitalPct()).isEqualTo("24.000000");assertThat(r.alertSeverity()).isEqualTo("HIGH");
 }
 @Test void zeroSeverityAndFullRecoveryProduceZeroLoss(){
  assertThat(ScenarioService.calculate(new BigDecimal("100"),capital("1000"),thresholds(),request("0","0")).stressedLoss()).isEqualTo("0.00");
  assertThat(ScenarioService.calculate(new BigDecimal("100"),capital("1000"),thresholds(),request("100","100")).stressedLoss()).isEqualTo("0.00");
 }
 @Test void missingOrNonpositiveCapitalAndInvalidPercentAreRejected(){
  assertThatThrownBy(()->ScenarioService.calculate(new BigDecimal("1"),capital("0"),thresholds(),request("60","30"))).isInstanceOf(IllegalArgumentException.class);
  assertThatThrownBy(()->ScenarioService.calculate(new BigDecimal("1"),capital("100"),thresholds(),request("100.01","30"))).isInstanceOf(IllegalArgumentException.class);
 }
}
