package com.nbfc.accessdata.data.reference;

import java.util.List;

import org.springframework.data.repository.Repository;

public interface LendingReferenceLoanRepository
        extends Repository<LendingReferenceLoan, String> {

    List<LendingReferenceLoan>
        findAllByReferenceIdOrderByNbfcIdAscLoanIdAsc(
            String referenceId
        );
}