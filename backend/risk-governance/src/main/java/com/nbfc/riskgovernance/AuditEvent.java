package com.nbfc.riskgovernance;
import jakarta.persistence.*;
import java.time.Instant;
@Entity
@Table(name="risk_audit_event")
public class AuditEvent {
 @Id @Column(length=36) public String id;
 @Column(name="entity_type",nullable=false,length=40) public String entityType;
 @Column(name="entity_id",nullable=false,length=36) public String entityId;
 @Column(nullable=false,length=60) public String action;
 @Column(nullable=false,length=255) public String actor;
 @Column(length=2000) public String details;
 @Column(name="created_at",nullable=false) public Instant createdAt;
 protected AuditEvent() {}
 public AuditEvent(String id,String type,String entityId,String action,String actor,String details) { this.id=id;this.entityType=type;this.entityId=entityId;this.action=action;this.actor=actor;this.details=details;this.createdAt=Instant.now(); }
}

