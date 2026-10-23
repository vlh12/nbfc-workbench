package com.nbfc.accessdata.data.reference;

import java.util.List;
import java.util.Optional;

import org.springframework.data.repository.Repository;

public interface LendingReferenceRepository
        extends Repository<LendingReference, String> {

    List<LendingReference>
        findAllByBankIdOrderByReportingDateDescReferenceVersionDesc(
            String bankId
        );

    Optional<LendingReference> findByIdAndBankId(
        String id,
        String bankId
    );
}