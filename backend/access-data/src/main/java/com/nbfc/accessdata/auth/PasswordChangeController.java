package com.nbfc.accessdata.auth;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nbfc.accessdata.auth.LoginChallengeService.VerifiedLogin;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.UUID;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import org.springframework.dao.DataAccessException;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.logout.CookieClearingLogoutHandler;
import org.springframework.transaction.TransactionException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/auth/password")
public class PasswordChangeController {

    private static final String PENDING_CHANGE =
            "WB_PASSWORD_CHANGE_PROOF";

    private static final String PROOF_CONSUMED =
            "WB_PASSWORD_CHANGE_CONSUMED";

    private static final int VERIFICATION_SECONDS = 300;

    private final LoginChallengeService credentials;
    private final PasswordChangeService passwordChanges;
    private final ObjectMapper json;

    public PasswordChangeController(
            LoginChallengeService credentials,
            PasswordChangeService passwordChanges,
            ObjectMapper json) {

        this.credentials = credentials;
        this.passwordChanges = passwordChanges;
        this.json = json;
    }

    @PostMapping("/check")
    public PasswordCheckResponse check(
            @RequestBody PasswordCheckRequest input,
            HttpServletRequest request) {

        requireSignedOut();

        if (input == null) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Enter your username and current password."
            );
        }

        HttpSession session = request.getSession(true);

        session.removeAttribute(PENDING_CHANGE);
        session.removeAttribute("WB_AUTH_PENDING_CHALLENGE");
        session.removeAttribute("WB_AUTH_BROWSER_BINDING");

        VerifiedLogin account = credentials.checkCredentials(
                input.username(),
                input.currentPassword(),
                input.role()
        );

        String flowId = UUID.randomUUID().toString();

        long expiresAtEpochMillis =
                System.currentTimeMillis()
                + VERIFICATION_SECONDS * 1000L;

        var proof = new PendingChange(
                flowId,
                account,
                expiresAtEpochMillis
        );

        String encodedProof;

        try {
            encodedProof = json.writeValueAsString(proof);
        } catch (JsonProcessingException failure) {
            throw new ResponseStatusException(
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    "Could not prepare password-change verification. "
                    + "Please try again."
            );
        }

        request.changeSessionId();

        // Store a String so every service can read the shared session.
        session.setAttribute(PENDING_CHANGE, encodedProof);

        return new PasswordCheckResponse(
                flowId,
                account.username(),
                account.role(),
                VERIFICATION_SECONDS
        );
    }

    @PostMapping("/change")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void change(
            @RequestBody PasswordChangeRequest input,
            HttpServletRequest request,
            HttpServletResponse response) {

        requireSignedOut();

        if (input == null) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Enter and confirm your new password."
            );
        }

        HttpSession session = request.getSession(false);

        if (session == null) {
            throw invalidVerification();
        }

        PendingChange proof = readProof(session);

        if (!sameFlow(proof.flowId(), input.flowId())) {
            // Preserve a newer verification belonging to another tab.
            throw invalidVerification();
        }

        PasswordChangeService.validateNewPassword(
                input.newPassword(),
                input.confirmNewPassword()
        );

        request.setAttribute(PROOF_CONSUMED, Boolean.TRUE);

        // Consume the pending session before changing the password.
        try {
            session.invalidate();
        } catch (IllegalStateException failure) {
            throw invalidVerification();
        } finally {
            new CookieClearingLogoutHandler("WBSESSION")
                    .logout(request, response, null);
        }

        passwordChanges.changePassword(
                proof.account(),
                proof.expiresAtEpochMillis(),
                input.newPassword(),
                input.confirmNewPassword()
        );
    }

    private PendingChange readProof(HttpSession session) {
        Object stored;

        try {
            stored = session.getAttribute(PENDING_CHANGE);
        } catch (IllegalStateException failure) {
            throw invalidVerification();
        }

        if (!(stored instanceof String encodedProof)) {
            throw invalidVerification();
        }

        PendingChange proof;

        try {
            proof = json.readValue(encodedProof, PendingChange.class);
        } catch (JsonProcessingException failure) {
            throw invalidVerification();
        }

        if (proof == null
                || proof.flowId() == null
                || proof.flowId().length() != 36
                || proof.account() == null
                || proof.account().userId() == null
                || proof.expiresAtEpochMillis()
                        <= System.currentTimeMillis()) {

            throw invalidVerification();
        }

        return proof;
    }

    private static boolean sameFlow(
            String expected,
            String supplied) {

        if (supplied == null
                || supplied.length() != expected.length()) {
            return false;
        }

        return MessageDigest.isEqual(
                expected.getBytes(StandardCharsets.UTF_8),
                supplied.getBytes(StandardCharsets.UTF_8)
        );
    }

    private static void requireSignedOut() {
        Authentication current =
                SecurityContextHolder.getContext().getAuthentication();

        if (current != null
                && current.isAuthenticated()
                && !(current instanceof AnonymousAuthenticationToken)) {

            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "You are already signed in. Please log out first."
            );
        }
    }

    private static ResponseStatusException invalidVerification() {
        return new ResponseStatusException(
                HttpStatus.UNAUTHORIZED,
                "Password-change verification is missing, expired, "
                + "or replaced. Verify your current credentials again."
        );
    }

    @ExceptionHandler({
            BadCredentialsException.class,
            ResponseStatusException.class,
            DataAccessException.class,
            TransactionException.class
    })
    public ResponseEntity<ErrorMessage> passwordError(
            RuntimeException failure,
            HttpServletRequest request) {

        boolean consumed = Boolean.TRUE.equals(
                request.getAttribute(PROOF_CONSUMED)
        );

        int status;
        String message;
        boolean restartRequired = consumed;

        if (failure instanceof BadCredentialsException) {
            status = HttpStatus.UNAUTHORIZED.value();

            message =
                    "Invalid username, password, or selected role.";

        } else if (failure instanceof ResponseStatusException problem) {
            status = problem.getStatusCode().value();

            message = problem.getReason() == null
                    ? "The password-change request could not be completed."
                    : problem.getReason();

            restartRequired = consumed || status == 401 || status == 409;

        } else if (failure instanceof OptimisticLockingFailureException) {
            status = HttpStatus.CONFLICT.value();

            message =
                    "The account changed. Verify your current "
                    + "credentials again.";

            restartRequired = true;

        } else {
            status = HttpStatus.SERVICE_UNAVAILABLE.value();

            message = consumed
                    ? "Could not confirm the password change. "
                      + "Please try signing in before starting again."
                    : "The account service is temporarily unavailable. "
                      + "Please try again.";
        }

        return ResponseEntity
                .status(status)
                .body(new ErrorMessage(message, restartRequired));
    }

    public record PasswordCheckRequest(
            String username,
            String currentPassword,
            ApplicationRole role) {

        @Override
        public String toString() {
            return "PasswordCheckRequest[credentials=<redacted>]";
        }
    }

    public record PasswordChangeRequest(
            String flowId,
            String newPassword,
            String confirmNewPassword) {

        @Override
        public String toString() {
            return "PasswordChangeRequest[credentials=<redacted>]";
        }
    }

    public record PasswordCheckResponse(
            String flowId,
            String username,
            String role,
            int expiresInSeconds) {
    }

    public record PendingChange(
            String flowId,
            VerifiedLogin account,
            long expiresAtEpochMillis) {
    }

    public record ErrorMessage(
            String message,
            boolean restartRequired) {
    }
}