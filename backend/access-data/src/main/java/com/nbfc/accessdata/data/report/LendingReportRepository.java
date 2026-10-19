package com.nbfc.accessdata.data.report;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface LendingReportRepository
        extends JpaRepository<LendingReport, String> {

    // Contributor: latest 50 reports submitted by that contributor.
    List<LendingReport>
        findTop50ByBankIdAndSubmittedByOrderByCreatedAtDescIdDesc(
            String bankId,
            String submittedBy
        );

    // Contributor: one report belonging to that contributor and bank.
    Optional<LendingReport> findByIdAndBankIdAndSubmittedBy(
            String id,
            String bankId,
            String submittedBy
    );

    // Reviewer: reports for one bank, returned one page at a time.
    Page<LendingReport> findByBankIdOrderByCreatedAtDescIdDesc(
            String bankId,
            Pageable pageable
    );

    // Reviewer: one saved report belonging to the selected bank.
    Optional<LendingReport> findByIdAndBankId(
            String id,
            String bankId
    );
}