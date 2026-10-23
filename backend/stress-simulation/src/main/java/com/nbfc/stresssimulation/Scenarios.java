package com.nbfc.stresssimulation;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.*;
public interface Scenarios extends JpaRepository<ScenarioRecord,String>{
    Optional<ScenarioRecord> findByBankIdAndCreatedByAndRequestKey(String bank,String actor,String key);
    Optional<ScenarioRecord> findByIdAndBankId(String id,String bank);
    List<ScenarioRecord> findTop100ByBankIdOrderByCreatedAtDesc(String bank);
}
