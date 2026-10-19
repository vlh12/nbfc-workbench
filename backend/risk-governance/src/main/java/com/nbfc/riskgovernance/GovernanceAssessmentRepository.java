package com.nbfc.riskgovernance;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
interface GovernanceAssessmentRepository extends JpaRepository<GovernanceAssessment,String> {
    List<GovernanceAssessment> findTop100ByOrderBySubmittedAtDesc();
    List<GovernanceAssessment> findTop100BySubmittedByOrderBySubmittedAtDesc(String submittedBy);
    List<GovernanceAssessment> findTop100ByStatusOrderBySubmittedAtAsc(AssessmentStatus status);
}

