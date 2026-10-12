package com.nbfc.exposureanalytics;
import java.util.List;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
@RestController
@RequestMapping("/api/exposures")
public class ExposureController {
    private final ExposureRepository repository;
    public ExposureController(ExposureRepository repository) { this.repository=repository; }
    @GetMapping("/summary")
    @PreAuthorize("hasAnyRole('ANALYST','SUPERVISOR')")
    public List<NbfcTotal> summary() { return repository.totals(); }
}

