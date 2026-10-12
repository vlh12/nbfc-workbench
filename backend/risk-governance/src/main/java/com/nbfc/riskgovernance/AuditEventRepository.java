package com.nbfc.riskgovernance;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
interface AuditEventRepository extends JpaRepository<AuditEvent,String> {
    List<AuditEvent> findTop200ByOrderByOccurredAtDesc();
    List<AuditEvent> findTop100ByEntityTypeAndEntityIdOrderByOccurredAtDesc(String entityType,String entityId);
}

