package com.nbfc.accessdata.data.reference;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.LocalDate;

@Entity
@Table(name = "lending_reference")
public class LendingReference {

    @Id
    @Column(length = 36)
    private String id;

    @Column(name = "bank_id", nullable = false, length = 44)
    private String bankId;

    @Column(name = "bank_name", nullable = false, length = 120)
    private String bankName;

    @Column(name = "reporting_date", nullable = false)
    private LocalDate reportingDate;

    @Column(name = "reference_version", nullable = false)
    private int referenceVersion;

    @Column(name = "amount_basis", nullable = false, length = 30)
    private String amountBasis;

    @Column(nullable = false, length = 3)
    private String currency;

    @Column(name = "money_unit", nullable = false, length = 10)
    private String unit;

    protected LendingReference() {
    }

    public String getId() {
        return id;
    }

    public String getBankId() {
        return bankId;
    }

    public String getBankName() {
        return bankName;
    }

    public LocalDate getReportingDate() {
        return reportingDate;
    }

    public int getReferenceVersion() {
        return referenceVersion;
    }

    public String getAmountBasis() {
        return amountBasis;
    }

    public String getCurrency() {
        return currency;
    }

    public String getUnit() {
        return unit;
    }
}