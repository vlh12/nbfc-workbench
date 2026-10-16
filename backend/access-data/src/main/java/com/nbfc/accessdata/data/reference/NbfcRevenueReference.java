package com.nbfc.accessdata.data.reference;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "nbfc_revenue_reference")
public class NbfcRevenueReference {

    @Id
    @Column(name = "id", length = 36)
    private String id;

    @Column(name = "reference_id", nullable = false, length = 36)
    private String referenceId;

    @Column(name = "nbfc_id", nullable = false, length = 44)
    private String nbfcId;

    @Column(name = "period_start", nullable = false)
    private LocalDate periodStart;

    @Column(name = "period_end", nullable = false)
    private LocalDate periodEnd;

    @Column(name = "revenue_amount", nullable = false, precision = 18, scale = 2)
    private BigDecimal revenueAmount;

    protected NbfcRevenueReference() {
    }

    public String getId() {
        return id;
    }

    public String getReferenceId() {
        return referenceId;
    }

    public String getNbfcId() {
        return nbfcId;
    }

    public LocalDate getPeriodStart() {
        return periodStart;
    }

    public LocalDate getPeriodEnd() {
        return periodEnd;
    }

    public BigDecimal getRevenueAmount() {
        return revenueAmount;
    }
}