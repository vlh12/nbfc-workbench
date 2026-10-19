package com.nbfc.accessdata.data.portfolio;
import jakarta.persistence.*;
import java.time.*;
@Entity @Table(name="portfolio_document")
public class PortfolioDocument {
    @Id @Column(length=36) public String id;
    @Column(nullable=false,length=20) public String kind;
    @Column(name="bank_id",nullable=false,length=44) public String bankId;
    @Column(name="reporting_date") public LocalDate reportingDate;
    @Column(nullable=false,length=20) public String status;
    @Column(name="submitted_by",nullable=false,length=255) public String submittedBy;
    @Column(name="created_at",nullable=false) public Instant createdAt;
    @Column(name="row_count",nullable=false) public int rowCount;
    @Lob @Column(nullable=false) public String payload;
    protected PortfolioDocument(){}
}
