package com.nbfc.accessdata.access;

import com.nbfc.accessdata.data.report.LendingReportCsvValidator.InvalidCsv;
import com.nbfc.accessdata.data.report.LendingReportCsvValidator.Issue;
import com.nbfc.accessdata.data.report.LendingReportService;
import com.nbfc.accessdata.data.report.LendingReportValidationService;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.net.URI;
import java.util.List;

@RestController
@RequestMapping("/api/lending-reports")
public class LendingReportController {

    private final LendingReportValidationService validationService;
    private final LendingReportService reportService;

    public LendingReportController(
            LendingReportValidationService validationService,
            LendingReportService reportService) {

        this.validationService = validationService;
        this.reportService = reportService;
    }

    public record Failure(
            String status,
            String message,
            List<Issue> issues) {
    }

    @PostMapping(
            value = "/validate",
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE
    )
    @PreAuthorize("hasRole('CONTRIBUTOR')")
    public LendingReportValidationService.Result validate(
            @RequestParam("referenceId") String referenceId,
            @RequestParam("file") MultipartFile file,
            @RequestParam(
                    value = "synthetic",
                    defaultValue = "false"
            ) boolean synthetic) throws IOException {

        checkUpload(referenceId, synthetic);

        return validationService.validate(
                referenceId,
                file.getBytes()
        );
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasRole('CONTRIBUTOR')")
    public ResponseEntity<LendingReportService.Detail> save(
            @RequestParam("referenceId") String referenceId,
            @RequestParam("file") MultipartFile file,
            @RequestParam(
                    value = "synthetic",
                    defaultValue = "false"
            ) boolean synthetic,
            Authentication authentication) throws IOException {

        checkUpload(referenceId, synthetic);

        LendingReportService.Detail saved = reportService.save(
                referenceId,
                file.getBytes(),
                file.getOriginalFilename(),
                authentication.getName()
        );

        URI location = URI.create(
                "/api/lending-reports/" + saved.report().id()
        );

        return ResponseEntity.created(location).body(saved);
    }

    @GetMapping
    @PreAuthorize("hasRole('CONTRIBUTOR')")
    public List<LendingReportService.Summary> list(
            Authentication authentication) {

        return reportService.list(authentication.getName());
    }

    @GetMapping("/{reportId}")
    @PreAuthorize("hasRole('CONTRIBUTOR')")
    public LendingReportService.Detail detail(
            @PathVariable("reportId") String reportId,
            Authentication authentication) {

        return reportService.detail(
                reportId,
                authentication.getName()
        );
    }

    @GetMapping("/review")
    @PreAuthorize("hasAnyRole('ANALYST', 'SUPERVISOR')")
    public LendingReportService.ReportPage reviewList(
            @RequestParam(
                    value = "page",
                    defaultValue = "0"
            ) int page,
            @RequestParam(
                    value = "size",
                    defaultValue = "20"
            ) int size) {

        return reportService.listForReview(page, size);
    }

    @GetMapping("/review/{reportId}")
    @PreAuthorize("hasAnyRole('ANALYST', 'SUPERVISOR')")
    public LendingReportService.Detail reviewDetail(
            @PathVariable("reportId") String reportId) {

        return reportService.detailForReview(reportId);
    }

    private void checkUpload(
            String referenceId,
            boolean synthetic) {

        if (!synthetic) {
            throw new InvalidCsv(List.of(
                    new Issue(
                            0,
                            "Confirm that the file contains synthetic data only."
                    )
            ));
        }

        if (referenceId == null || referenceId.isBlank()) {
            throw new InvalidCsv(List.of(
                    new Issue(
                            0,
                            "Select a bank reference before validating."
                    )
            ));
        }
    }

    @ExceptionHandler(InvalidCsv.class)
    public ResponseEntity<Failure> invalid(InvalidCsv error) {

        return ResponseEntity.unprocessableEntity().body(
                new Failure(
                        "INVALID",
                        error.getMessage(),
                        error.issues
                )
        );
    }

    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<Failure> referenceError(
            ResponseStatusException error) {

        String message = error.getReason() != null
                ? error.getReason()
                : "The request could not be completed.";

        return ResponseEntity.status(error.getStatusCode()).body(
                new Failure(
                        "INVALID",
                        message,
                        List.of()
                )
        );
    }
}