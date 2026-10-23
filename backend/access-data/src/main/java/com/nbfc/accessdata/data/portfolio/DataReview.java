package com.nbfc.accessdata.data.portfolio;
import jakarta.persistence.*;
import java.time.Instant;
@Entity @Table(name="portfolio_data_review")
public class DataReview {
 @Id @Column(length=36) public String id;
 @Column(name="document_id",nullable=false,length=36) public String documentId;
 @Column(nullable=false,length=20) public String decision;
 @Column(name="reviewed_by",nullable=false,length=255) public String reviewedBy;
 @Column(name="reviewed_at",nullable=false) public Instant reviewedAt;
 @Column(name="review_note",nullable=false,length=2000) public String reviewNote;
 public DataReview(){}
}
