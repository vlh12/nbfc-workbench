package com.nbfc.riskgovernance;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

interface AuditEventRepository extends JpaRepository<AuditEvent, String> {
    
    // Updated from OccurredAt to CreatedAt
    List<AuditEvent> findTop200ByOrderByCreatedAtDesc();
    
    // Updated from OccurredAt to CreatedAt
    List<AuditEvent> findTop100ByEntityTypeAndEntityIdOrderByCreatedAtDesc(String entityType, String entityId);
}
