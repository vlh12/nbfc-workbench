package com.nbfc.riskgovernance;
import jakarta.persistence.*;
import java.time.Instant;
@Entity
@Table(name="risk_assessment")
public class Assessment {
 @Id @Column(length=36) public String id;
 @Column(name="batch_id",nullable=false,length=36) public String batchId;
 @Column(name="submitted_by",nullable=false,length=255) public String submittedBy;
 @Column(nullable=false,length=20) public String status;
 @Column(length=2000) public String submissionNote;
 @Column(length=2000) public String decisionNote;
 @Column(name="decided_by",length=255) public String decidedBy;
 @Column(name="submitted_at",nullable=false) public Instant submittedAt;
 @Column(name="decided_at") public Instant decidedAt;
 @Column(name="scenario_id",length=36) public String scenarioId;
 @Lob @Column(name="scenario_evidence") public String scenarioEvidence;
 @Version public long version;
 protected Assessment() {}
 public Assessment(String id,String batchId,String submittedBy,String note) { this.id=id;this.batchId=batchId;this.submittedBy=submittedBy;this.submissionNote=note;this.status="SUBMITTED";this.submittedAt=Instant.now(); }
 public boolean canDecide() { return "SUBMITTED".equals(status) || "RETURNED".equals(status); }
}

