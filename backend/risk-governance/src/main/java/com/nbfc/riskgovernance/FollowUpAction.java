package com.nbfc.riskgovernance;
import jakarta.persistence.*;
import java.time.*;
@Entity
@Table(name="risk_follow_up")
public class FollowUpAction {
 @Id @Column(length=36) public String id;
 @Column(name="assessment_id",nullable=false,length=36) public String assessmentId;
 @Column(nullable=false,length=500) public String title;
 @Column(nullable=false,length=255) public String owner;
 @Column(name="due_date",nullable=false) public LocalDate dueDate;
 @Column(nullable=false,length=20) public String status;
 @Column(name="created_by",nullable=false,length=255) public String createdBy;
 @Column(name="created_at",nullable=false) public Instant createdAt;
 @Column(name="updated_at",nullable=false) public Instant updatedAt;
 @Column(length=2000) public String evidence;
 @Version public long version;
 protected FollowUpAction() {}
 public FollowUpAction(String id,String assessmentId,String title,String owner,LocalDate dueDate,String actor) { this.id=id;this.assessmentId=assessmentId;this.title=title;this.owner=owner;this.dueDate=dueDate;this.status="OPEN";this.createdBy=actor;this.createdAt=Instant.now();this.updatedAt=this.createdAt; }
}

