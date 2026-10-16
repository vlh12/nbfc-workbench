package com.nbfc.accessdata.data.reference;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.math.BigDecimal;

@Entity
@Table(name = "lending_reference_loan")
public class LendingReferenceLoan {

    @Id
    @Column(name = "id", length = 36)
    private String id;

    @Column(name = "reference_id", nullable = false, length = 36)
    private String referenceId;

    @Column(name = "loan_id", nullable = false, length = 64)
    private String loanId;

    @Column(name = "nbfc_id", nullable = false, length = 44)
    private String nbfcId;

    @Column(name = "nbfc_name", nullable = false, length = 120)
    private String nbfcName;

    @Column(name = "amount_lent", nullable = false, precision = 18, scale = 2)
    private BigDecimal amountLent;

    @Column(name = "interest_amount", nullable = false, precision = 18, scale = 2)
    private BigDecimal interestAmount;

    @Column(name = "amount_repaid", nullable = false, precision = 18, scale = 2)
    private BigDecimal amountRepaid;

    protected LendingReferenceLoan() {
    }

    public String getId() {
        return id;
    }

    public String getReferenceId() {
        return referenceId;
    }

    public String getLoanId() {
        return loanId;
    }

    public String getNbfcId() {
        return nbfcId;
    }

    public String getNbfcName() {
        return nbfcName;
    }

    public BigDecimal getAmountLent() {
        return amountLent;
    }

    public BigDecimal getInterestAmount() {
        return interestAmount;
    }

    public BigDecimal getAmountRepaid() {
        return amountRepaid;
    }
}