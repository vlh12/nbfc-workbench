package com.nbfc.shared;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

public final class PortfolioData {
    private PortfolioData() {}
    public static final String BANK = "SYN-BANK-001"; // Local demo scope; never a production tenant claim.
    public record Loan(String bankId, String nbfcId, String loanId, LocalDate reportingDate,
        String instrumentType, String amountLent, String interestAmount, String totalPayable,
        String amountRepaid, String outstandingAmount, Integer dpdDays, String loanStatus,
        LocalDate maturityDate, String collateralType, LocalDate revenuePeriodStart,
        LocalDate revenuePeriodEnd, String revenueAmount, String currency, String unit, String eadAmount) {}
    public record Issue(int line, String field, String message) {}
    public record Validation(List<Loan> rows, List<Issue> warnings, List<Issue> errors) {}
    public record Snapshot(String id, String bankId, LocalDate reportingDate, String status,
        String submittedBy, Instant createdAt, List<Loan> rows, List<Issue> warnings) {}
    public record Entry(String id, String kind, String bankId, LocalDate reportingDate,
        String status, String submittedBy, Instant createdAt, int rowCount) {}
    public record Capital(String bankId, LocalDate reportingDate, String eligibleCapital, String label) {}
    public record Quality(String bankId, String nbfcId, LocalDate reportingDate,
        Map<String, String> buckets, String totalAmount) {}
    public record Thresholds(String bankId, String name, String mediumPct, String highPct) {}
    public record Nbfc(String nbfcId, int loans, String amountLent, String outstandingAmount,
        String eadAmount, String portfolioSharePct, Map<String, String> loanStatusAmounts) {}
    public record Analysis(String snapshotId, String bankId, LocalDate reportingDate, String status,
        int loans, String totalLent, String totalOutstanding, String totalEad, String concentrationDenominator,
        List<Nbfc> nbfcs, List<Issue> warnings) {}
}
