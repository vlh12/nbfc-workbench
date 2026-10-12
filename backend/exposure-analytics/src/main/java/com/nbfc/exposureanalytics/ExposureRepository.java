package com.nbfc.exposureanalytics;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
public interface ExposureRepository extends JpaRepository<ExposureRecord,String> {
    @Query("select new com.nbfc.exposureanalytics.NbfcTotal(e.nbfcId,sum(e.exposureAmount),count(e)) from ExposureRecord e group by e.nbfcId order by sum(e.exposureAmount) desc")
    List<NbfcTotal> totals();
}

