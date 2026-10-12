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
  if(!item.canDecide()) throw new IllegalStateException("Only submitted or returned assessments can be decided.");
  if(!"APPROVED".equals(decision) && !"RETURNED".equals(decision)) throw new IllegalArgumentException("Decision must be APPROVED or RETURNED.");
  item.status=decision; item.decisionNote=note; item.decidedBy=actor; item.decidedAt=java.time.Instant.now();
  record("ASSESSMENT",id,decision,actor,note); return assessments.save(item);
 }
 @Transactional public FollowUpAction addFollowUp(String assessmentId,String title,String owner,LocalDate dueDate,String actor) {
  var assessment=assessments.findById(assessmentId).orElseThrow();
  var action=followUps.save(new FollowUpAction(UUID.randomUUID().toString(),assessment.id,title,owner,dueDate,actor));
  record("FOLLOW_UP",action.id,"CREATED",actor,title); return action;
 }
 @Transactional public FollowUpAction updateFollowUp(String id,String status,String actor) {
  var action=followUps.findById(id).orElseThrow();
  if(!"OPEN".equals(status) && !"COMPLETED".equals(status)) throw new IllegalArgumentException("Status must be OPEN or COMPLETED.");
  action.status=status; action.updatedAt=java.time.Instant.now(); record("FOLLOW_UP",id,status,actor,null); return followUps.save(action);
 }
 private void record(String type,String id,String action,String actor,String details){audit.save(new AuditEvent(UUID.randomUUID().toString(),type,id,action,actor,details));}
}

