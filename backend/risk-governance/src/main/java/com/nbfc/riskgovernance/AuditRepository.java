package com.nbfc.riskgovernance;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
public interface AuditRepository extends JpaRepository<AuditEvent,String> { List<AuditEvent> findTop200ByOrderByCreatedAtDesc(); }

