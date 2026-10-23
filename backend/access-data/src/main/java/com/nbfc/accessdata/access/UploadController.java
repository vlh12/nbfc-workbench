package com.nbfc.accessdata.access;
import com.nbfc.accessdata.data.*;
import java.io.IOException;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;
@io.swagger.v3.oas.annotations.tags.Tag(name="Legacy exposure batches",description="Three-column exposure uploads and batch status.")
@RestController
@RequestMapping("/api/batches")
public class UploadController {
    private final UploadService service;
    private final BatchRepository batches;
    public UploadController(UploadService service,BatchRepository batches) { this.service=service; this.batches=batches; }
    @PostMapping(consumes="multipart/form-data")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('CONTRIBUTOR')")
    public Batch upload(@RequestParam MultipartFile file,@RequestParam boolean synthetic,
                        Authentication authentication) throws IOException {
        if (!synthetic) throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Confirm synthetic data before uploading.");
        return service.upload(file.getBytes(),authentication.getName());
    }
    @GetMapping
    @PreAuthorize("hasAnyRole('CONTRIBUTOR','ANALYST','SUPERVISOR')")
    public List<Batch> list(Authentication authentication) {
        boolean reviewer=authentication.getAuthorities().stream().map(GrantedAuthority::getAuthority)
            .anyMatch(a -> a.equals("ROLE_ANALYST") || a.equals("ROLE_SUPERVISOR"));
        return reviewer ? batches.findTop50ByOrderByCreatedAtDesc()
            : batches.findTop50BySubmittedByOrderByCreatedAtDesc(authentication.getName());
    }
}
