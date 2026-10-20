package com.nbfc.accessdata.access;

import com.nbfc.accessdata.data.reference.LendingReferenceService;

import java.util.List;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@io.swagger.v3.oas.annotations.tags.Tag(name="Lending reference data",description="Legacy reference-based contributor inputs.")
@RestController
@RequestMapping("/api/lending-references")
@PreAuthorize("hasRole('CONTRIBUTOR')")
public class LendingReferenceController {

    private final LendingReferenceService service;

    public LendingReferenceController(
            LendingReferenceService service) {
        this.service = service;
    }

    @GetMapping
    public List<LendingReferenceService.Summary> list() {
        return service.list();
    }

    @GetMapping("/{referenceId}")
    public LendingReferenceService.Detail detail(
            @PathVariable("referenceId") String referenceId) {
        return service.detail(referenceId);
    }
}