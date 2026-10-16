package com.nbfc.accessdata.data.report;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface LendingReportLoanRepository
        extends JpaRepository<LendingReportLoan, String> {

    List<LendingReportLoan> findAllByReportIdOrderByLoanIdAsc(
            String reportId
    );
}