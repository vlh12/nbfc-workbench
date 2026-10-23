package com.nbfc.riskgovernance;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/governance")
public class ScenarioReviewReportController {
    private final AssessmentRepository assessments;
    private final FollowUpRepository followUps;
    private final AuditRepository audit;
    public ScenarioReviewReportController(AssessmentRepository assessments, FollowUpRepository followUps, AuditRepository audit) {
        this.assessments=assessments; this.followUps=followUps; this.audit=audit;
    }
    public record Submission(String id,String scenarioId,String snapshotId,String submittedBy,Instant submittedAt,
            String submissionNote,String status,String decidedBy,Instant decidedAt,String decisionNote) {}
    public record History(String scenarioId,List<Submission> assessments,List<FollowUpAction> followUps,List<AuditEvent> events) {}

    // This is an exact, read-only scenario lookup, not the dashboard's latest-100 queue.
    @GetMapping("/scenarios/{scenarioId}/review-history")
    @PreAuthorize("hasRole('SUPERVISOR')")
    @Transactional(readOnly=true)
    public History history(@PathVariable String scenarioId) {
        if(!UUID.fromString(scenarioId).toString().equals(scenarioId)) throw new IllegalArgumentException("Provide a valid scenario reference.");
        var matches=assessments.findByScenarioIdOrderBySubmittedAtAscIdAsc(scenarioId);
        if(matches.isEmpty())return new History(scenarioId,List.of(),List.of(),List.of());
        var ids=matches.stream().map(item->item.id).toList();
        var actions=followUps.findByAssessmentIdInOrderByDueDateAscIdAsc(ids);
        var events=new ArrayList<>(audit.findByEntityTypeAndEntityIdInOrderByCreatedAtAscIdAsc("ASSESSMENT",ids));
        if(!actions.isEmpty()) events.addAll(audit.findByEntityTypeAndEntityIdInOrderByCreatedAtAscIdAsc("FOLLOW_UP",actions.stream().map(item->item.id).toList()));
        events.sort(Comparator.comparing((AuditEvent event)->event.createdAt).thenComparing(event->event.id));
        var summaries=matches.stream().map(item->new Submission(item.id,item.scenarioId,item.batchId,item.submittedBy,
                item.submittedAt,item.submissionNote,item.status,item.decidedBy,item.decidedAt,item.decisionNote)).toList();
        return new History(scenarioId,summaries,actions,List.copyOf(events));
    }
}
