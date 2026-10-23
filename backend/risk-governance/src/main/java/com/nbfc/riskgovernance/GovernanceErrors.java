package com.nbfc.riskgovernance;
import java.util.Map;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
@RestControllerAdvice
class GovernanceErrors {
    private final AuditRepository audit;
    GovernanceErrors(AuditRepository audit){this.audit=audit;}
    @ExceptionHandler(org.springframework.security.access.AccessDeniedException.class)
    ResponseEntity<?> forbidden(org.springframework.security.access.AccessDeniedException error,
            jakarta.servlet.http.HttpServletRequest request) {
        var authentication=org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication();
        String actor=authentication==null?"anonymous":authentication.getName();
        audit.save(new AuditEvent(java.util.UUID.randomUUID().toString(),"ACCESS",
            java.util.UUID.randomUUID().toString(),"UNAUTHORIZED_ATTEMPT",actor,request.getMethod()+" "+request.getRequestURI()));
        return ResponseEntity.status(403).body(Map.of("message","Your role cannot perform this action."));
    }

    @ExceptionHandler(IllegalArgumentException.class)
    ResponseEntity<?> argument(IllegalArgumentException e){return ResponseEntity.unprocessableEntity().body(Map.of("message",e.getMessage()));}
    @ExceptionHandler(java.util.NoSuchElementException.class)
    ResponseEntity<?> missing(java.util.NoSuchElementException e){return ResponseEntity.status(404).body(Map.of("message","Record not found."));}
    @ExceptionHandler({IllegalStateException.class,org.springframework.orm.ObjectOptimisticLockingFailureException.class})
    ResponseEntity<?> conflict(RuntimeException e){return ResponseEntity.status(409).body(Map.of("message","The record changed or this transition is no longer allowed. Refresh before retrying."));}
    @ExceptionHandler(GovernanceException.class)
    ResponseEntity<?> governance(GovernanceException error) {
        return ResponseEntity.status(error.status).body(Map.of("message",error.getMessage()));
    }
    @ExceptionHandler(MethodArgumentNotValidException.class)
    ResponseEntity<?> invalid(MethodArgumentNotValidException error) {
        String message=error.getBindingResult().getFieldErrors().stream().findFirst()
            .map(field -> field.getField()+" "+field.getDefaultMessage()).orElse("Request is invalid.");
        return ResponseEntity.badRequest().body(Map.of("message",message));
    }
}

