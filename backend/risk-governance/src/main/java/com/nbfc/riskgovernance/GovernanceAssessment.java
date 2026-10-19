package com.nbfc.riskgovernance;
import jakarta.persistence.*;
import java.time.Instant;
@Entity
@Table(name="governance_assessment")
public class GovernanceAssessment {
    @Id @Column(length=36) public String id;
    @Column(nullable=false,length=160) public String title;
    @Column(nullable=false,length=2000) public String summary;
    @Enumerated(EnumType.STRING) @Column(nullable=false,length=20) public AssessmentStatus status;
    @Column(name="submitted_by",nullable=false,length=255) public String submittedBy;
    @Column(name="submitted_at",nullable=false) public Instant submittedAt;
    @Column(name="decision_note",length=2000) public String decisionNote;
    @Column(name="reviewed_by",length=255) public String reviewedBy;
    @Column(name="reviewed_at") public Instant reviewedAt;
    protected GovernanceAssessment() {}
    GovernanceAssessment(String id,String title,String summary,String actor) {
        this.id=id; this.title=title; this.summary=summary; this.status=AssessmentStatus.SUBMITTED;
        this.submittedBy=actor; this.submittedAt=Instant.now();
    }
}

