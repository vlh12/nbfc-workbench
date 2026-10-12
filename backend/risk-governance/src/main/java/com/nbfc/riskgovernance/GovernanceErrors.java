package com.nbfc.riskgovernance;
import java.util.Map;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
@RestControllerAdvice
class GovernanceErrors {
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

