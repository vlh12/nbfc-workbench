package com.nbfc.accessdata.auth;

import java.util.List;
import java.util.UUID;

import com.nbfc.shared.auth.WorkbenchPrincipal;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.logout.CookieClearingLogoutHandler;
import org.springframework.security.web.authentication.logout.SecurityContextLogoutHandler;
import org.springframework.security.web.authentication.session.SessionAuthenticationStrategy;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private static final String PENDING_CHALLENGE =
            "WB_AUTH_PENDING_CHALLENGE";

    private static final String BROWSER_BINDING =
            "WB_AUTH_BROWSER_BINDING";

    private final LoginChallengeService challenges;
    private final HttpSessionSecurityContextRepository contexts;
    private final SessionAuthenticationStrategy sessionStrategy;

    public AuthController(
            LoginChallengeService challenges,
            HttpSessionSecurityContextRepository contexts,
            @Qualifier("workbenchLoginSessionStrategy")
            SessionAuthenticationStrategy sessionStrategy) {

        this.challenges = challenges;
        this.contexts = contexts;
        this.sessionStrategy = sessionStrategy;
    }

    @GetMapping("/csrf")
    public CsrfResponse csrf(CsrfToken token) {
        return new CsrfResponse(
                token.getHeaderName(),
                token.getToken());
    }

    @PostMapping("/login")
    public LoginCodeResponse login(
            @RequestBody LoginRequest input,
            HttpServletRequest request) {

        requireSignedOut();

        HttpSession session = request.getSession(true);

        clearPendingLogin(session);

        String binding = UUID.randomUUID().toString();

        var issued = challenges.begin(
                input.username(),
                input.password(),
                input.role(),
                binding);

        session.setAttribute(
                PENDING_CHALLENGE,
                issued.challengeId());

        session.setAttribute(
                BROWSER_BINDING,
                binding);

        return new LoginCodeResponse(
                issued.displayedCode(),
                issued.expiresInSeconds());
    }

    @PostMapping("/verify")
    public Profile verify(
            @RequestBody VerificationRequest input,
            HttpServletRequest request,
            HttpServletResponse response) {

        requireSignedOut();

        HttpSession session = request.getSession(false);

        if (session == null) {
            throw invalidChallenge();
        }

        Object pending = session.getAttribute(PENDING_CHALLENGE);
        Object browser = session.getAttribute(BROWSER_BINDING);

        if (!(pending instanceof String challengeId)
                || !(browser instanceof String binding)) {

            throw invalidChallenge();
        }

        var verified = challenges.verify(
                challengeId,
                input.code(),
                binding)
                .orElseThrow(AuthController::invalidChallenge);

        clearPendingLogin(session);

        var principal = new WorkbenchPrincipal(
                verified.userId(),
                verified.username(),
                verified.role(),
                verified.accountVersion());

        var authentication =
                UsernamePasswordAuthenticationToken.authenticated(
                        principal,
                        null,
                        List.of(new SimpleGrantedAuthority(
                                "ROLE_" + verified.role())));

        sessionStrategy.onAuthentication(
                authentication,
                request,
                response);

        var context = SecurityContextHolder.createEmptyContext();

        context.setAuthentication(authentication);

        SecurityContextHolder.setContext(context);

        contexts.saveContext(context, request, response);

        return profile(principal);
    }

    @GetMapping("/me")
    public Profile me(
            @AuthenticationPrincipal WorkbenchPrincipal principal) {

        if (principal == null) {
            throw new ResponseStatusException(
                    HttpStatus.UNAUTHORIZED,
                    "Please sign in.");
        }

        return profile(principal);
    }

    @PostMapping("/logout")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void logout(
            HttpServletRequest request,
            HttpServletResponse response,
            Authentication authentication) {

        var logoutHandler = new SecurityContextLogoutHandler();

        logoutHandler.setSecurityContextRepository(contexts);

        logoutHandler.logout(
                request,
                response,
                authentication);

        new CookieClearingLogoutHandler("WBSESSION")
                .logout(request, response, authentication);
    }

    @ExceptionHandler({
            BadCredentialsException.class,
            ResponseStatusException.class
    })
    public ResponseEntity<ErrorMessage> authenticationError(
            RuntimeException exception) {

        if (exception instanceof ResponseStatusException statusError) {
            String message = statusError.getReason() == null
                    ? "Authentication request failed."
                    : statusError.getReason();

            return ResponseEntity
                    .status(statusError.getStatusCode())
                    .body(new ErrorMessage(message));
        }

        return ResponseEntity
                .status(HttpStatus.UNAUTHORIZED)
                .body(new ErrorMessage(
                        "Invalid username, password, or selected role."));
    }

    private static void requireSignedOut() {
        Authentication current =
                SecurityContextHolder.getContext().getAuthentication();

        if (current != null
                && current.isAuthenticated()
                && !(current instanceof AnonymousAuthenticationToken)) {

            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "You are already signed in. Please log out first.");
        }
    }

    private static void clearPendingLogin(HttpSession session) {
        session.removeAttribute(PENDING_CHALLENGE);
        session.removeAttribute(BROWSER_BINDING);
    }

    private static ResponseStatusException invalidChallenge() {
        return new ResponseStatusException(
                HttpStatus.UNAUTHORIZED,
                "The login code is incorrect, expired, or unavailable.");
    }

    private static Profile profile(WorkbenchPrincipal principal) {
        return new Profile(
                principal.userId(),
                principal.username(),
                principal.role());
    }

    public record LoginRequest(
            String username,
            String password,
            ApplicationRole role) {
    }

    public record VerificationRequest(String code) {
    }

    public record LoginCodeResponse(
            String code,
            int expiresInSeconds) {
    }

    public record CsrfResponse(
            String headerName,
            String token) {
    }

    public record Profile(
            String id,
            String username,
            String role) {
    }

    public record ErrorMessage(String message) {
    }
}