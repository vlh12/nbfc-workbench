package com.nbfc.riskgovernance;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;
import java.time.*;
import java.util.*;
class Phase0GovernanceTest {
 @Test void creatorCannotSelfApprove(){
  var a=mock(AssessmentRepository.class);var item=new Assessment("a","b","creator","note");
  when(a.findById("a")).thenReturn(Optional.of(item));
  var service=new GovernanceService(a,mock(FollowUpRepository.class),mock(AuditRepository.class));
  assertThatThrownBy(()->service.decide("a","APPROVED","approve","creator")).isInstanceOf(IllegalArgumentException.class);
  verify(a,never()).save(any());
 }
 @Test void evidenceRequiredBeforeVerificationAndClosedActionIsImmutable(){
  var a=mock(AssessmentRepository.class);var f=mock(FollowUpRepository.class);var audit=mock(AuditRepository.class);
  var action=new FollowUpAction("f","a","Check","owner",LocalDate.now(),"supervisor");
  when(f.findById("f")).thenReturn(Optional.of(action));when(f.save(any())).thenAnswer(i->i.getArgument(0));
  var service=new GovernanceService(a,f,audit);
  service.updateFollowUp("f","IN_PROGRESS",null,"supervisor");
  assertThatThrownBy(()->service.updateFollowUp("f","EVIDENCE_SUBMITTED",null,"supervisor")).isInstanceOf(IllegalArgumentException.class);
  service.updateFollowUp("f","EVIDENCE_SUBMITTED","Synthetic review completed","supervisor");
  service.updateFollowUp("f","VERIFIED",null,"supervisor");
  service.updateFollowUp("f","CLOSED",null,"supervisor");
  assertThat(action.evidence).isEqualTo("Synthetic review completed");
  assertThatThrownBy(()->service.updateFollowUp("f","OPEN",null,"supervisor")).isInstanceOf(IllegalArgumentException.class);
  verify(audit,times(4)).save(any());
 }
}
