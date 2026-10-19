package com.nbfc.accessdata.data;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
public interface OutboxRepository extends JpaRepository<OutboxEvent,String> {
    List<OutboxEvent> findTop20ByPublishedFalseOrderByCreatedAtAsc();
}

