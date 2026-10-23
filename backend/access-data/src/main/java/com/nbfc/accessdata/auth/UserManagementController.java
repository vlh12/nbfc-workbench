package com.nbfc.accessdata.auth;

import com.nbfc.shared.auth.WorkbenchPrincipal;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Map;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/auth/users")
public class UserManagementController {

    private final ApplicationUserRepository users;
    private final PasswordEncoder passwords;

    public UserManagementController(
            ApplicationUserRepository users,
            PasswordEncoder passwords) {

        this.users = users;
        this.passwords = passwords;
    }

    public record CreateUserRequest(
            String username,
            String password,
            ApplicationRole role) {

        @Override
        public String toString() {
            return "CreateUserRequest[username=" + username
                    + ", password=<redacted>, role=" + role + "]";
        }
    }

    public record UserView(
            String id,
            String username,
            ApplicationRole role,
            String status,
            String createdBy,
            Instant createdAt) {
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('SUPERVISOR')")
    public UserView createUser(
            @RequestBody CreateUserRequest request,
            @AuthenticationPrincipal WorkbenchPrincipal supervisor) {

        if (supervisor == null) {
            throw new ResponseStatusException(
                    HttpStatus.UNAUTHORIZED,
                    "Please sign in as a Supervisor."
            );
        }

        if (request == null) {
            throw badRequest("User details are required.");
        }

        String username;

        try {
            username = ApplicationUser.normalizeUsername(
                    request.username()
            );
        } catch (IllegalArgumentException failure) {
            throw badRequest(failure.getMessage());
        }

        if (request.role() == null) {
            throw badRequest(
                    "Choose Contributor, Analyst or Supervisor."
            );
        }

        validatePassword(request.password());

        if (users.findByUsername(username).isPresent()) {
            throw duplicateUsername();
        }

        String passwordHash = passwords.encode(request.password());

        var account = new ApplicationUser(
                username,
                passwordHash,
                request.role(),
                supervisor.userId()
        );

        ApplicationUser saved;

        try {
            saved = users.saveAndFlush(account);
        } catch (DataIntegrityViolationException failure) {
            // Handle another request creating the same username.
            if (users.findByUsername(username).isPresent()) {
                throw duplicateUsername();
            }

            throw failure;
        }

        return new UserView(
                saved.getId(),
                saved.getUsername(),
                saved.getRole(),
                saved.getStatus(),
                saved.getCreatedBy(),
                saved.getCreatedAt()
        );
    }

    private static void validatePassword(String password) {
        if (password == null
                || password.isBlank()
                || password.codePointCount(
                        0,
                        password.length()
                ) < 12) {

            throw badRequest(
                    "Initial password must contain at least 12 characters."
            );
        }

        if (password.getBytes(StandardCharsets.UTF_8).length > 72) {
            throw badRequest(
                    "Initial password is too long. "
                    + "Use no more than 72 UTF-8 bytes."
            );
        }
    }

    private static ResponseStatusException badRequest(String message) {
        return new ResponseStatusException(
                HttpStatus.BAD_REQUEST,
                message
        );
    }

    private static ResponseStatusException duplicateUsername() {
        return new ResponseStatusException(
                HttpStatus.CONFLICT,
                "That username is already in use."
        );
    }

    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<Map<String, String>> requestFailure(
            ResponseStatusException failure) {

        String message = failure.getReason();

        if (message == null || message.isBlank()) {
            message = "The user could not be created.";
        }

        return ResponseEntity
                .status(failure.getStatusCode())
                .body(Map.of("message", message));
    }
}