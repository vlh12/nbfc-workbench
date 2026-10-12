package com.nbfc.riskgovernance;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
interface GovernanceOutboxRepository extends JpaRepository<GovernanceOutboxEvent,String> {
    List<GovernanceOutboxEvent> findTop20ByPublishedFalseOrderByCreatedAtAsc();
}

