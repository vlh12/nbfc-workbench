package com.nbfc.accessdata.data.report;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.util.UUID;

@Entity
@Table(name = "lending_report_loan")
public class LendingReportLoan {

    @Id
    @Column(name = "id", nullable = false, length = 36)
    private String id;

    @Column(name = "report_id", nullable = false, length = 36)
    private String reportId;

    @Column(name = "loan_id", nullable = false, length = 64)
    private String loanId;

    @Column(name = "amount_lent", nullable = false, precision = 18, scale = 2)
    private BigDecimal amountLent;

    @Column(name = "interest_amount", nullable = false, precision = 18, scale = 2)
    private BigDecimal interestAmount;

    @Column(name = "amount_repaid", nullable = false, precision = 18, scale = 2)
    private BigDecimal amountRepaid;

    protected LendingReportLoan() {
    }

    public LendingReportLoan(
            String reportId,
            String loanId,
            BigDecimal amountLent,
            BigDecimal interestAmount,
            BigDecimal amountRepaid) {

        this.id = UUID.randomUUID().toString();
        this.reportId = reportId;
        this.loanId = loanId;
        this.amountLent = amountLent;
        this.interestAmount = interestAmount;
        this.amountRepaid = amountRepaid;
    }

    public String getId() { return id; }
    public String getReportId() { return reportId; }
    public String getLoanId() { return loanId; }
    public BigDecimal getAmountLent() { return amountLent; }
    public BigDecimal getInterestAmount() { return interestAmount; }
    public BigDecimal getAmountRepaid() { return amountRepaid; }
}