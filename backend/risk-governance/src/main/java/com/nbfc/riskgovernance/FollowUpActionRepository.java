package com.nbfc.riskgovernance;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
interface FollowUpActionRepository extends JpaRepository<FollowUpAction,String> {
    List<FollowUpAction> findTop100ByOrderByUpdatedAtDesc();
    List<FollowUpAction> findTop100ByAssessmentIdOrderByUpdatedAtDesc(String assessmentId);
}

