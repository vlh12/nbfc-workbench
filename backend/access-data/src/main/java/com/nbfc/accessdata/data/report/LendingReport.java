package com.nbfc.accessdata.data.report;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "lending_report")
public class LendingReport {

    @Id
    @Column(name = "id", nullable = false, length = 36)
    private String id;

    @Column(name = "reference_id", nullable = false, length = 36)
    private String referenceId;

    @Column(name = "bank_id", nullable = false, length = 44)
    private String bankId;

    @Column(name = "nbfc_id", nullable = false, length = 44)
    private String nbfcId;

    @Column(name = "nbfc_name", nullable = false, length = 120)
    private String nbfcName;

    @Column(name = "reporting_date", nullable = false)
    private LocalDate reportingDate;

    @Column(name = "revenue_period_start", nullable = false)
    private LocalDate revenuePeriodStart;

    @Column(name = "revenue_period_end", nullable = false)
    private LocalDate revenuePeriodEnd;

    @Column(name = "revenue_amount", nullable = false, precision = 18, scale = 2)
    private BigDecimal revenueAmount;

    @Column(name = "currency", nullable = false, length = 3)
    private String currency;

    @Column(name = "money_unit", nullable = false, length = 10)
    private String unit;

    @Column(name = "row_count", nullable = false)
    private int rowCount;

    @Column(name = "status", nullable = false, length = 30)
    private String status;

    @Column(name = "original_filename", nullable = false, length = 255)
    private String originalFilename;

    @Column(name = "submitted_by", nullable = false, length = 255)
    private String submittedBy;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected LendingReport() {
    }

    public LendingReport(
            LendingReportValidationService.Result validated,
            String originalFilename,
            String submittedBy) {

        if (validated == null
                || !"VALIDATED".equals(validated.status())) {
            throw new IllegalArgumentException(
                    "Only a successfully validated report can be saved."
            );
        }

        this.id = UUID.randomUUID().toString();
        this.referenceId = validated.referenceId();
        this.bankId = validated.bankId();
        this.nbfcId = validated.nbfcId();
        this.nbfcName = validated.nbfcName();
        this.reportingDate = validated.reportingDate();
        this.revenuePeriodStart = validated.revenuePeriodStart();
        this.revenuePeriodEnd = validated.revenuePeriodEnd();
        this.revenueAmount = validated.revenueAmount();
        this.currency = validated.currency();
        this.unit = validated.unit();
        this.rowCount = validated.rowCount();
        this.status = "VALIDATED";
        this.originalFilename = originalFilename;
        this.submittedBy = submittedBy;
        this.createdAt = Instant.now();
    }

    public String getId() { return id; }
    public String getReferenceId() { return referenceId; }
    public String getBankId() { return bankId; }
    public String getNbfcId() { return nbfcId; }
    public String getNbfcName() { return nbfcName; }
    public LocalDate getReportingDate() { return reportingDate; }
    public LocalDate getRevenuePeriodStart() { return revenuePeriodStart; }
    public LocalDate getRevenuePeriodEnd() { return revenuePeriodEnd; }
    public BigDecimal getRevenueAmount() { return revenueAmount; }
    public String getCurrency() { return currency; }
    public String getUnit() { return unit; }
    public int getRowCount() { return rowCount; }
    public String getStatus() { return status; }
    public String getOriginalFilename() { return originalFilename; }
    public String getSubmittedBy() { return submittedBy; }
    public Instant getCreatedAt() { return createdAt; }
}