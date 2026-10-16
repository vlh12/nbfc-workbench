package com.nbfc.accessdata.data.reference;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.nbfc.shared.DecimalStringSerializer;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Service
@Transactional(readOnly = true)
public class LendingReferenceService {

    private static final String OUR_BANK_ID = "SYN-BANK-001";

    private final LendingReferenceRepository referenceRepository;
    private final LendingReferenceLoanRepository loanRepository;
    private final NbfcRevenueReferenceRepository revenueRepository;

    public LendingReferenceService(
            LendingReferenceRepository referenceRepository,
            LendingReferenceLoanRepository loanRepository,
            NbfcRevenueReferenceRepository revenueRepository) {

        this.referenceRepository = referenceRepository;
        this.loanRepository = loanRepository;
        this.revenueRepository = revenueRepository;
    }

    public record Summary(
            String id,
            String bankId,
            String bankName,
            LocalDate reportingDate,
            int version,
            String amountBasis,
            String currency,
            String unit,
            boolean synthetic) {
    }

    public record Loan(
            String loanId,
            String nbfcId,
            String nbfcName,

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

    public record Revenue(
            String nbfcId,
            LocalDate periodStart,
            LocalDate periodEnd,

            @JsonSerialize(using = DecimalStringSerializer.class)
            BigDecimal revenueAmount) {
    }

    public record Detail(
            Summary reference,
            List<Loan> loans,
            List<Revenue> revenues) {
    }

    public List<Summary> list() {
        return referenceRepository
                .findAllByBankIdOrderByReportingDateDescReferenceVersionDesc(
                        OUR_BANK_ID)
                .stream()
                .map(this::toSummary)
                .toList();
    }

    public Detail detail(String referenceId) {
        LendingReference reference = referenceRepository
                .findByIdAndBankId(referenceId, OUR_BANK_ID)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "No lending reference was found for our bank."
                ));

        List<Loan> loans = loanRepository
                .findAllByReferenceIdOrderByNbfcIdAscLoanIdAsc(reference.getId())
                .stream()
                .map(this::toLoan)
                .toList();

        List<Revenue> revenues = revenueRepository
                .findAllByReferenceIdOrderByNbfcIdAscPeriodStartAscPeriodEndAsc(
                        reference.getId())
                .stream()
                .map(this::toRevenue)
                .toList();

        return new Detail(toSummary(reference), loans, revenues);
    }

    private Summary toSummary(LendingReference reference) {
        return new Summary(
                reference.getId(),
                reference.getBankId(),
                reference.getBankName(),
                reference.getReportingDate(),
                reference.getReferenceVersion(),
                reference.getAmountBasis(),
                reference.getCurrency(),
                reference.getUnit(),
                true
        );
    }

    private Loan toLoan(LendingReferenceLoan loan) {
        BigDecimal totalPayable = loan.getAmountLent()
                .add(loan.getInterestAmount());

        BigDecimal outstandingAmount = totalPayable
                .subtract(loan.getAmountRepaid());

        return new Loan(
                loan.getLoanId(),
                loan.getNbfcId(),
                loan.getNbfcName(),
                loan.getAmountLent(),
                loan.getInterestAmount(),
                totalPayable,
                loan.getAmountRepaid(),
                outstandingAmount
        );
    }

    private Revenue toRevenue(NbfcRevenueReference revenue) {
        return new Revenue(
                revenue.getNbfcId(),
                revenue.getPeriodStart(),
                revenue.getPeriodEnd(),
                revenue.getRevenueAmount()
        );
    }
}