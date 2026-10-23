package com.nbfc.accessdata.data.portfolio;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
public interface DataReviews extends JpaRepository<DataReview,String> {
 List<DataReview> findByDocumentIdOrderByReviewedAtAscIdAsc(String documentId);
}
