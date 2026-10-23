package com.nbfc.stresssimulation;
import jakarta.persistence.*;
import java.time.Instant;
@Entity @Table(name="stress_run")
public class ScenarioRecord {
    @Id @Column(length=36) public String id;
    @Column(name="bank_id",nullable=false,length=44) public String bankId;
    @Column(name="created_by",nullable=false,length=255) public String createdBy;
    @Column(name="created_at",nullable=false) public Instant createdAt;
    @Column(name="request_key",nullable=false,length=64) public String requestKey;
    @Column(name="request_hash",nullable=false,length=64) public String requestHash;
    @Lob @Column(nullable=false) public String payload;
    protected ScenarioRecord(){}
}
