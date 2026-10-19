package com.nbfc.exposureanalytics;
import org.springframework.data.jpa.repository.JpaRepository;
public interface ProcessedRepository extends JpaRepository<ProcessedEvent,String> {}

