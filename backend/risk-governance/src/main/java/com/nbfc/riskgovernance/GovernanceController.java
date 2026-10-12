package com.nbfc.riskgovernance;
import java.time.LocalDate;
import java.util.List;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
@RestController
@RequestMapping("/api/governance")
public class GovernanceController {
 private final GovernanceService service; private final AssessmentRepository assessments; private final FollowUpRepository followUps; private final AuditRepository audit;
 public GovernanceController(GovernanceService s,AssessmentRepository a,FollowUpRepository f,AuditRepository audit){service=s;assessments=a;followUps=f;this.audit=audit;}
 public record Submission(String batchId,String note) {}
 public record Decision(String decision,String note) {}
 public record FollowUpRequest(String title,String owner,LocalDate dueDate) {}
 public record FollowUpUpdate(String status) {}
 @GetMapping("/assessments") @PreAuthorize("hasRole('SUPERVISOR')") public List<Assessment> assessments(){return assessments.findTop100ByOrderBySubmittedAtDesc();}
 @GetMapping("/assessments/queue") @PreAuthorize("hasRole('SUPERVISOR')") public List<Assessment> queue(){return assessments.findTop100ByOrderBySubmittedAtDesc();}
 @PostMapping("/assessments") @PreAuthorize("hasRole('ANALYST')") public Assessment submit(@RequestBody Submission body,Authentication actor){return service.submit(body.batchId(),body.note(),actor.getName());}
 @PostMapping("/assessments/{id}/decision") @PreAuthorize("hasRole('SUPERVISOR')") public Assessment decide(@PathVariable String id,@RequestBody Decision body,Authentication actor){return service.decide(id,body.decision(),body.note(),actor.getName());}
 @PostMapping("/assessments/{id}/approve") @PreAuthorize("hasRole('SUPERVISOR')") public Assessment approve(@PathVariable String id,@RequestBody Decision body,Authentication actor){return service.decide(id,"APPROVED",body.note(),actor.getName());}
 @GetMapping("/follow-ups") @PreAuthorize("hasRole('SUPERVISOR')") public List<FollowUpAction> followUps(){return followUps.findTop100ByOrderByDueDateAsc();}
 @PostMapping("/assessments/{id}/follow-ups") @PreAuthorize("hasRole('SUPERVISOR')") public FollowUpAction add(@PathVariable String id,@RequestBody FollowUpRequest body,Authentication actor){return service.addFollowUp(id,body.title(),body.owner(),body.dueDate(),actor.getName());}
 @PatchMapping("/follow-ups/{id}") @PreAuthorize("hasRole('SUPERVISOR')") public FollowUpAction update(@PathVariable String id,@RequestBody FollowUpUpdate body,Authentication actor){return service.updateFollowUp(id,body.status(),actor.getName());}
 @GetMapping("/audit") @PreAuthorize("hasRole('SUPERVISOR')") public List<AuditEvent> audit(){return audit.findTop200ByOrderByCreatedAtDesc();}
}
