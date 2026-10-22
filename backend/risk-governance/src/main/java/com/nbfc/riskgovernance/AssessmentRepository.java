package com.nbfc.riskgovernance;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
public interface AssessmentRepository extends JpaRepository<Assessment,String> {
 List<Assessment> findTop100BySubmittedByOrderBySubmittedAtDesc(String submittedBy); List<Assessment> findTop100ByOrderBySubmittedAtDesc(); 
 List<Assessment> findByScenarioIdOrderBySubmittedAtAscIdAsc(String scenarioId);
}

