package com.nbfc.accessdata.access;
import com.nbfc.accessdata.data.CsvValidator;
import java.util.Map;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
@RestControllerAdvice
public class UploadErrors {
    @ExceptionHandler(CsvValidator.InvalidCsv.class)
    ResponseEntity<?> invalid(CsvValidator.InvalidCsv error) {
        return ResponseEntity.unprocessableEntity().body(Map.of("message",error.getMessage(),"issues",error.issues));
    }
    @ExceptionHandler(MaxUploadSizeExceededException.class)
    ResponseEntity<?> tooLarge() {
        return ResponseEntity.status(413).body(Map.of("message","Upload a CSV smaller than 2 MB."));
    }
}

