package com.nbfc.accessdata.data;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
public interface BatchRepository extends JpaRepository<Batch,String> {
    List<Batch> findTop50ByOrderByCreatedAtDesc();
    List<Batch> findTop50BySubmittedByOrderByCreatedAtDesc(String actor);
}

