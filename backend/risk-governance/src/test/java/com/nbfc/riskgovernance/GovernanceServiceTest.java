package com.nbfc.riskgovernance;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;
class GovernanceServiceTest {
 @Test void supervisorDecisionAndAuditAreRecorded() {
  var assessments=mock(AssessmentRepository.class); var followUps=mock(FollowUpRepository.class); var audit=mock(AuditRepository.class);
  var item=new Assessment("a1","b1","analyst","note"); when(assessments.findById("a1")).thenReturn(java.util.Optional.of(item)); when(assessments.save(any())).thenAnswer(i->i.getArgument(0));
  var service=new GovernanceService(assessments,followUps,audit);
  var result=service.decide("a1","APPROVED","accepted","supervisor");
  assertThat(result.status).isEqualTo("APPROVED"); assertThat(result.decidedBy).isEqualTo("supervisor"); verify(audit).save(argThat(e->e.action.equals("APPROVED")&&e.actor.equals("supervisor")));
 }
 @Test void returnedAssessmentCanBeDecidedAgain() {
  var assessments=mock(AssessmentRepository.class); var followUps=mock(FollowUpRepository.class); var audit=mock(AuditRepository.class);
  var item=new Assessment("a1","b1","analyst","note"); item.status="RETURNED"; when(assessments.findById("a1")).thenReturn(java.util.Optional.of(item)); when(assessments.save(any())).thenAnswer(i->i.getArgument(0));
  var result=new GovernanceService(assessments,followUps,audit).decide("a1","APPROVED","fixed","supervisor");
  assertThat(result.status).isEqualTo("APPROVED");
 }
 @Test void followUpOnlyAllowsOpenAndCompleted() {
  var assessments=mock(AssessmentRepository.class); var followUps=mock(FollowUpRepository.class); var audit=mock(AuditRepository.class);
  var item=new Assessment("a1","b1","analyst","note"); when(assessments.findById("a1")).thenReturn(java.util.Optional.of(item));
  when(followUps.save(any())).thenAnswer(i->i.getArgument(0));
  var action=new GovernanceService(assessments,followUps,audit).addFollowUp("a1","Call bank","owner",LocalDate.now().plusDays(7),"supervisor");
  assertThat(action.status).isEqualTo("OPEN"); verify(audit).save(any());
 }
}

