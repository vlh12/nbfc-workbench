package com.nbfc.accessdata.data.report;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.nbfc.accessdata.data.reference.LendingReference;
import com.nbfc.accessdata.data.reference.LendingReferenceLoan;
import com.nbfc.accessdata.data.reference.LendingReferenceLoanRepository;
import com.nbfc.accessdata.data.reference.LendingReferenceRepository;
import com.nbfc.accessdata.data.reference.NbfcRevenueReference;
import com.nbfc.accessdata.data.reference.NbfcRevenueReferenceRepository;
import com.nbfc.accessdata.data.report.LendingReportCsvValidator.InvalidCsv;
import com.nbfc.accessdata.data.report.LendingReportCsvValidator.Issue;
import com.nbfc.accessdata.data.report.LendingReportCsvValidator.Row;
import com.nbfc.shared.DecimalStringSerializer;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
@Transactional(readOnly = true)
public class LendingReportValidationService {

    private static final String OUR_BANK_ID = "SYN-BANK-001";
    private static final int MAX_ISSUES = 100;

    private final LendingReportCsvValidator csvValidator;
    private final LendingReferenceRepository referenceRepository;
    private final LendingReferenceLoanRepository loanRepository;
    private final NbfcRevenueReferenceRepository revenueRepository;

    public LendingReportValidationService(
            LendingReportCsvValidator csvValidator,
            LendingReferenceRepository referenceRepository,
            LendingReferenceLoanRepository loanRepository,
            NbfcRevenueReferenceRepository revenueRepository) {

        this.csvValidator = csvValidator;
        this.referenceRepository = referenceRepository;
        this.loanRepository = loanRepository;
        this.revenueRepository = revenueRepository;
    }

    public record Result(
            String status,
            String message,
            String referenceId,
            String bankId,
            String nbfcId,
            String nbfcName,
            LocalDate reportingDate,
            int rowCount,
            LocalDate revenuePeriodStart,
            LocalDate revenuePeriodEnd,

            @JsonSerialize(using = DecimalStringSerializer.class)
            BigDecimal revenueAmount,

            String currency,
            String unit) {
    }

    public Result validate(String referenceId, byte[] bytes) {
        LendingReference reference = referenceRepository
                .findByIdAndBankId(referenceId, OUR_BANK_ID)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "The selected reference was not found for our bank."
                ));

        List<Row> rows = csvValidator.validate(bytes);
        Row first = rows.get(0);
        String nbfcId = first.nbfcId();

        Map<String, LendingReferenceLoan> expectedLoans =
                new LinkedHashMap<>();

        for (LendingReferenceLoan loan : loanRepository
                .findAllByReferenceIdOrderByNbfcIdAscLoanIdAsc(
                        reference.getId())) {

            if (loan.getNbfcId().equals(nbfcId)) {
                expectedLoans.put(loan.getLoanId(), loan);
            }
        }

        if (expectedLoans.isEmpty()) {
            throw new InvalidCsv(List.of(new Issue(
                    first.line(),
                    "nbfc_id " + nbfcId
                            + " has no loans in the selected bank reference."
            )));
        }

        List<Issue> issues = new ArrayList<>();
        Set<String> seenLoanIds = new HashSet<>();
        Set<String> presentLoanIds = new HashSet<>();

        for (Row row : rows) {
            if (!row.bankId().equals(reference.getBankId())) {
                addIssue(
                        issues,
                        row.line(),
                        "bank_id must be " + reference.getBankId()
                                + " for the selected reference."
                );
            }

            if (!row.reportingDate().equals(reference.getReportingDate())) {
                addIssue(
                        issues,
                        row.line(),
                        "reporting_date must be "
                                + reference.getReportingDate()
                                + " for the selected reference."
                );
            }

            if (!row.currency().equals(reference.getCurrency())) {
                addIssue(
                        issues,
                        row.line(),
                        "currency does not match the selected reference."
                );
            }

            if (!row.unit().equals(reference.getUnit())) {
                addIssue(
                        issues,
                        row.line(),
                        "unit does not match the selected reference."
                );
            }

            if (!row.nbfcId().equals(nbfcId)) {
                addIssue(
                        issues,
                        row.line(),
                        "Use one NBFC per report. Every row must use "
                                + nbfcId + "."
                );
                continue;
            }

            if (!row.revenuePeriodStart().equals(first.revenuePeriodStart())
                    || !row.revenuePeriodEnd().equals(first.revenuePeriodEnd())
                    || row.revenueAmount().compareTo(
                            first.revenueAmount()) != 0) {

                addIssue(
                        issues,
                        row.line(),
                        "Every row must use the same revenue period "
                                + "and revenue_amount for this NBFC."
                );
            }

            if (!seenLoanIds.add(row.loanId())) {
                addIssue(
                        issues,
                        row.line(),
                        "Duplicate loan_id: " + row.loanId()
                                + ". Include each loan only once."
                );
                continue;
            }

            LendingReferenceLoan expected =
                    expectedLoans.get(row.loanId());

            if (expected == null) {
                addIssue(
                        issues,
                        row.line(),
                        "loan_id " + row.loanId()
                                + " is not listed for " + nbfcId
                                + " in the selected reference."
                );
                continue;
            }

            presentLoanIds.add(row.loanId());

            BigDecimal expectedTotalPayable = expected.getAmountLent()
                    .add(expected.getInterestAmount());

            BigDecimal expectedOutstanding = expectedTotalPayable
                    .subtract(expected.getAmountRepaid());

            compareAmount(
                    issues, row.line(), "amount_lent",
                    row.amountLent(), expected.getAmountLent(), "bank"
            );

            compareAmount(
                    issues, row.line(), "interest_amount",
                    row.interestAmount(), expected.getInterestAmount(), "bank"
            );

            compareAmount(
                    issues, row.line(), "total_payable",
                    row.totalPayable(), expectedTotalPayable, "bank"
            );

            compareAmount(
                    issues, row.line(), "amount_repaid",
                    row.amountRepaid(), expected.getAmountRepaid(), "bank"
            );

            compareAmount(
                    issues, row.line(), "outstanding_amount",
                    row.outstandingAmount(), expectedOutstanding, "bank"
            );
        }

        for (String expectedLoanId : expectedLoans.keySet()) {
            if (!presentLoanIds.contains(expectedLoanId)) {
                addIssue(
                        issues,
                        0,
                        "Missing loan " + expectedLoanId
                                + ". Include all loans for " + nbfcId
                                + " from the selected reference."
                );
            }
        }

        List<NbfcRevenueReference> matchingRevenues = revenueRepository
                .findAllByReferenceIdOrderByNbfcIdAscPeriodStartAscPeriodEndAsc(
                        reference.getId())
                .stream()
                .filter(revenue -> revenue.getNbfcId().equals(nbfcId))
                .filter(revenue -> revenue.getPeriodStart().equals(
                        first.revenuePeriodStart()))
                .filter(revenue -> revenue.getPeriodEnd().equals(
                        first.revenuePeriodEnd()))
                .toList();

        if (matchingRevenues.size() != 1) {
            addIssue(
                    issues,
                    first.line(),
                    "Exactly one revenue reference is required for "
                            + nbfcId + " from "
                            + first.revenuePeriodStart() + " to "
                            + first.revenuePeriodEnd()
                            + ". Matching references found: "
                            + matchingRevenues.size() + "."
            );
        } else {
            compareAmount(
                    issues,
                    first.line(),
                    "revenue_amount",
                    first.revenueAmount(),
                    matchingRevenues.get(0).getRevenueAmount(),
                    "company revenue"
            );
        }

        if (!issues.isEmpty()) {
            throw new InvalidCsv(issues);
        }

        String nbfcName = expectedLoans.values()
                .iterator()
                .next()
                .getNbfcName();

        return new Result(
                "VALIDATED",
                "All loan and revenue figures match the selected reference records.",
                reference.getId(),
                reference.getBankId(),
                nbfcId,
                nbfcName,
                reference.getReportingDate(),
                rows.size(),
                first.revenuePeriodStart(),
                first.revenuePeriodEnd(),
                first.revenueAmount(),
                reference.getCurrency(),
                reference.getUnit()
        );
    }

    private static void compareAmount(
            List<Issue> issues,
            int line,
            String field,
            BigDecimal uploaded,
            BigDecimal expected,
            String source) {

        if (uploaded.compareTo(expected) != 0) {
            addIssue(
                    issues,
                    line,
                    field + " differs from the " + source
                            + " reference. Expected "
                            + expected.toPlainString()
                            + " INR; uploaded "
                            + uploaded.toPlainString() + " INR."
            );
        }
    }

    private static void addIssue(
            List<Issue> issues,
            int line,
            String message) {

        if (issues.size() < MAX_ISSUES) {
            issues.add(new Issue(line, message));
        }
    }
}