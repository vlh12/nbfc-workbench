package com.nbfc.accessdata.data.portfolio;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.*;
public interface PortfolioDocuments extends JpaRepository<PortfolioDocument,String>{
    List<PortfolioDocument> findTop100ByBankIdOrderByCreatedAtDescIdDesc(String bankId);
    Optional<PortfolioDocument> findByIdAndBankId(String id,String bankId);
}
