package com.nbfc.accessdata.data;
import jakarta.persistence.*;
import java.time.Instant;
@Entity
@Table(name = "exposure_batch")
public class Batch {
    @Id @Column(length=36) public String id;
    @Column(name="submitted_by",nullable=false,length=255) public String submittedBy;
    @Column(name="row_count",nullable=false) public int rowCount;
    @Column(nullable=false,length=30) public String status;
    @Column(name="created_at",nullable=false) public Instant createdAt;
    protected Batch() {}
    public Batch(String id, String actor, int count) {
        this.id=id; submittedBy=actor; rowCount=count; status="VALIDATED"; createdAt=Instant.now();
    }
}

