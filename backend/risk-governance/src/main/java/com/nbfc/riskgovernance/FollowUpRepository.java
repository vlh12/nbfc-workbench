package com.nbfc.riskgovernance;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
public interface FollowUpRepository extends JpaRepository<FollowUpAction,String> { List<FollowUpAction> findByAssessmentIdOrderByDueDateAsc(String id); List<FollowUpAction> findTop100ByOrderByDueDateAsc(); 
 List<FollowUpAction> findByAssessmentIdInOrderByDueDateAscIdAsc(List<String> ids);
}

