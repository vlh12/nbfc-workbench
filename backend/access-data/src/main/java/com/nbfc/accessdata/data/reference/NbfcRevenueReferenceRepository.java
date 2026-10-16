package com.nbfc.accessdata.data.reference;

import org.springframework.data.repository.Repository;

import java.util.List;

public interface NbfcRevenueReferenceRepository
        extends Repository<NbfcRevenueReference, String> {

    List<NbfcRevenueReference>
        findAllByReferenceIdOrderByNbfcIdAscPeriodStartAscPeriodEndAsc(
            String referenceId
        );
}