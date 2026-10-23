package com.nbfc.riskgovernance;
import java.time.LocalDate;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
@Service
public class GovernanceService {
 private final AssessmentRepository assessments; private final FollowUpRepository followUps; private final AuditRepository audit;
 public GovernanceService(AssessmentRepository a,FollowUpRepository f,AuditRepository audit){this.assessments=a;this.followUps=f;this.audit=audit;}
 @Transactional public Assessment submit(String batchId,String note,String actor) {
  var item=assessments.save(new Assessment(UUID.randomUUID().toString(),batchId,actor,note));
  record("ASSESSMENT",item.id,"SUBMITTED",actor,note); return item;
 }
 @Transactional public Assessment decide(String id,String decision,String note,String actor) {
  var item=assessments.findById(id).orElseThrow();
  if(item.submittedBy.equals(actor)) throw new IllegalArgumentException("Scenario creator cannot approve their own assessment.");
  if(note==null||note.isBlank()||note.length()>2000) throw new IllegalArgumentException("Provide a decision reason up to 2000 characters.");
  if(!item.canDecide()) throw new IllegalStateException("Only submitted or returned assessments can be decided.");
  if(!"APPROVED".equals(decision) && !"RETURNED".equals(decision)) throw new IllegalArgumentException("Decision must be APPROVED or RETURNED.");
  item.status=decision; item.decisionNote=note; item.decidedBy=actor; item.decidedAt=java.time.Instant.now();
  record("ASSESSMENT",id,decision,actor,note); return assessments.save(item);
 }
 @Transactional public FollowUpAction addFollowUp(String assessmentId,String title,String owner,LocalDate dueDate,String actor) {
  var assessment=assessments.findById(assessmentId).orElseThrow();
  if(title==null||title.isBlank()||title.length()>500||owner==null||owner.isBlank()||owner.length()>255||dueDate==null) throw new IllegalArgumentException("Provide a title, owner and due date.");
  var action=followUps.save(new FollowUpAction(UUID.randomUUID().toString(),assessment.id,title,owner,dueDate,actor));
  record("FOLLOW_UP",action.id,"CREATED",actor,title); return action;
 }
 @Transactional public FollowUpAction updateFollowUp(String id,String status,String actor) {
  return updateFollowUp(id,status,null,actor);
 }
 @Transactional public FollowUpAction updateFollowUp(String id,String status,String evidence,String actor) {
  var action=followUps.findById(id).orElseThrow();
  var transitions=java.util.Map.of(
   "OPEN",java.util.Set.of("IN_PROGRESS","COMPLETED"),
   "COMPLETED",java.util.Set.of("OPEN"),
   "IN_PROGRESS",java.util.Set.of("EVIDENCE_SUBMITTED"),
   "EVIDENCE_SUBMITTED",java.util.Set.of("VERIFIED","REOPENED"),
   "VERIFIED",java.util.Set.of("CLOSED"),
   "REOPENED",java.util.Set.of("IN_PROGRESS"),
   "CLOSED",java.util.Set.<String>of());
  if(status==null||!transitions.getOrDefault(action.status,java.util.Set.of()).contains(status)) throw new IllegalArgumentException("Invalid follow-up transition.");
  if("EVIDENCE_SUBMITTED".equals(status)&&(evidence==null||evidence.isBlank()||evidence.length()>2000)) throw new IllegalArgumentException("Provide evidence up to 2000 characters.");
  String before=action.status; action.status=status;
  if("EVIDENCE_SUBMITTED".equals(status))action.evidence=evidence;
  action.updatedAt=java.time.Instant.now(); record("FOLLOW_UP",id,status,actor,before+" -> "+status);
  return followUps.save(action);
 }
 @Transactional public Assessment submitScenario(com.nbfc.shared.StressData.Run run,String note,String actor) {
  if(!"COMPLETED".equals(run.status())||run.result()==null) throw new IllegalArgumentException("Only completed scenarios can be submitted.");
  if(!actor.equals(run.createdBy())) throw new IllegalArgumentException("Only the scenario creator can submit this run.");
  if(note==null||note.isBlank()||note.length()>2000) throw new IllegalArgumentException("Provide an assessment note.");
  var item=new Assessment(UUID.randomUUID().toString(),run.snapshotId(),actor,note);
  item.scenarioId=run.id();
  try {item.scenarioEvidence=new com.fasterxml.jackson.databind.ObjectMapper().findAndRegisterModules().writeValueAsString(run);}
  catch(com.fasterxml.jackson.core.JsonProcessingException e){throw new IllegalStateException(e);}
  assessments.save(item); record("ASSESSMENT",item.id,"SCENARIO_SUBMITTED",actor,run.id()); return item;
 }
 private void record(String type,String id,String action,String actor,String details){audit.save(new AuditEvent(UUID.randomUUID().toString(),type,id,action,actor,details));}
}

