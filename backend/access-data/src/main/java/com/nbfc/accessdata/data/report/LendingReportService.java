package com.nbfc.accessdata.data.report;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.nbfc.accessdata.data.report.LendingReportCsvValidator.Row;
import com.nbfc.shared.DecimalStringSerializer;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.access.prepost.PreAuthorize;

@Service
@Transactional(readOnly = true)
public class LendingReportService {

    private static final String OUR_BANK_ID = "SYN-BANK-001";

    private final LendingReportValidationService validationService;
    private final LendingReportCsvValidator csvValidator;
    private final LendingReportRepository reports;
    private final LendingReportLoanRepository loans;

    public LendingReportService(
            LendingReportValidationService validationService,
            LendingReportCsvValidator csvValidator,
            LendingReportRepository reports,
            LendingReportLoanRepository loans) {

        this.validationService = validationService;
        this.csvValidator = csvValidator;
        this.reports = reports;
        this.loans = loans;
    }

    public record Summary(
            String id,
            String referenceId,
            String bankId,
            String nbfcId,
            String nbfcName,
            LocalDate reportingDate,
            int rowCount,
            String status,
            LocalDate revenuePeriodStart,
            LocalDate revenuePeriodEnd,
            @JsonSerialize(using = DecimalStringSerializer.class)
            BigDecimal revenueAmount,
            String currency,
            String unit,
            String originalFilename,
            Instant createdAt) {
    }

    public record Loan(
            String loanId,
            @JsonSerialize(using = DecimalStringSerializer.class)
            BigDecimal amountLent,
            @JsonSerialize(using = DecimalStringSerializer.class)
            BigDecimal interestAmount,
            @JsonSerialize(using = DecimalStringSerializer.class)
            BigDecimal totalPayable,
            @JsonSerialize(using = DecimalStringSerializer.class)
            BigDecimal amountRepaid,
            @JsonSerialize(using = DecimalStringSerializer.class)
            BigDecimal outstandingAmount) {
    }

    public record Detail(
            Summary report,
            List<Loan> loans) {
    }

    @Transactional
    public Detail save(
            String referenceId,
            byte[] bytes,
            String originalFilename,
            String submittedBy) {

        checkContributor(submittedBy);
        String filename = cleanFilename(originalFilename);

        // Check the uploaded figures against the bank's records again.
        LendingReportValidationService.Result validated =
                validationService.validate(referenceId, bytes);

        // Read the loan rows from the same validated file.
        List<Row> rows = csvValidator.validate(bytes);

        LendingReport report = new LendingReport(
                validated,
                filename,
                submittedBy
        );

        // Save the report before saving its loan rows.
        LendingReport savedReport = reports.saveAndFlush(report);

        List<LendingReportLoan> reportLoans = rows.stream()
                .sorted(Comparator.comparing(Row::loanId))
                .map(row -> new LendingReportLoan(
                        savedReport.getId(),
                        row.loanId(),
                        row.amountLent(),
                        row.interestAmount(),
                        row.amountRepaid()
                ))
                .toList();

        List<LendingReportLoan> savedLoans =
                loans.saveAll(reportLoans);

        loans.flush();

        return new Detail(
                toSummary(savedReport),
                savedLoans.stream()
                        .map(this::toLoan)
                        .toList()
        );
    }

    public List<Summary> list(String submittedBy) {

        checkContributor(submittedBy);

        return reports
                .findTop50ByBankIdAndSubmittedByOrderByCreatedAtDescIdDesc(
                        OUR_BANK_ID,
                        submittedBy
                )
                .stream()
                .map(this::toSummary)
                .toList();
    }

    public Detail detail(String reportId, String submittedBy) {

        checkContributor(submittedBy);

        LendingReport report = reports
                .findByIdAndBankIdAndSubmittedBy(
                        reportId,
                        OUR_BANK_ID,
                        submittedBy
                )
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Saved report was not found."
                ));

        List<Loan> reportLoans = loans
                .findAllByReportIdOrderByLoanIdAsc(report.getId())
                .stream()
                .map(this::toLoan)
                .toList();

        return new Detail(
                toSummary(report),
                reportLoans
        );
    }

    public record ReportPage(
            List<Summary> content,
            int page,
            int size,
            long totalElements,
            int totalPages) {
    }

    @PreAuthorize("hasAnyRole('ANALYST', 'SUPERVISOR')")
    public ReportPage listForReview(int page, int size) {

        if (page < 0) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Page must be zero or greater."
            );
        }

        if (size < 1 || size > 50) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Page size must be between 1 and 50."
            );
        }

        Page<Summary> result = reports
                .findByBankIdOrderByCreatedAtDescIdDesc(
                        OUR_BANK_ID,
                        PageRequest.of(page, size)
                )
                .map(this::toSummary);

        return new ReportPage(
                result.getContent(),
                result.getNumber(),
                result.getSize(),
                result.getTotalElements(),
                result.getTotalPages()
        );
    }

    @PreAuthorize("hasAnyRole('ANALYST', 'SUPERVISOR')")
    public Detail detailForReview(String reportId) {

        LendingReport report = reports
                .findByIdAndBankId(
                        reportId,
                        OUR_BANK_ID
                )
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Saved report was not found."
                ));

        List<Loan> reportLoans = loans
                .findAllByReportIdOrderByLoanIdAsc(report.getId())
                .stream()
                .map(this::toLoan)
                .toList();

        return new Detail(
                toSummary(report),
                reportLoans
        );
    }

    private Summary toSummary(LendingReport report) {

        return new Summary(
                report.getId(),
                report.getReferenceId(),
                report.getBankId(),
                report.getNbfcId(),
                report.getNbfcName(),
                report.getReportingDate(),
                report.getRowCount(),
                report.getStatus(),
                report.getRevenuePeriodStart(),
                report.getRevenuePeriodEnd(),
                report.getRevenueAmount(),
                report.getCurrency(),
                report.getUnit(),
                report.getOriginalFilename(),
                report.getCreatedAt()
        );
    }

    private Loan toLoan(LendingReportLoan loan) {

        BigDecimal totalPayable = loan.getAmountLent()
                .add(loan.getInterestAmount());

        BigDecimal outstandingAmount = totalPayable
                .subtract(loan.getAmountRepaid());

        return new Loan(
                loan.getLoanId(),
                loan.getAmountLent(),
                loan.getInterestAmount(),
                totalPayable,
                loan.getAmountRepaid(),
                outstandingAmount
        );
    }

    private String cleanFilename(String originalFilename) {

        String filename = originalFilename == null
                ? ""
                : originalFilename.replace('\\', '/');

        filename = filename
                .substring(filename.lastIndexOf('/') + 1)
                .trim();

        if (filename.isBlank()) {
            return "lending-report.csv";
        }

        if (filename.length() > 255) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Use a filename with no more than 255 characters."
            );
        }

        return filename;
    }

    private void checkContributor(String submittedBy) {

        if (submittedBy == null
                || submittedBy.isBlank()
                || submittedBy.length() > 255) {

            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "A valid contributor name is required."
            );
        }
    }
}