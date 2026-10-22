package com.nbfc.shared.auth;

import java.io.IOException;
import java.util.Objects;
import java.util.Set;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.springframework.dao.DataAccessException;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.logout.CookieClearingLogoutHandler;
import org.springframework.security.web.authentication.logout.SecurityContextLogoutHandler;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.web.filter.OncePerRequestFilter;

public class AccountSessionFilter extends OncePerRequestFilter {

    private static final Set<String> ALLOWED_ROLES = Set.of(
            "CONTRIBUTOR",
            "ANALYST",
            "SUPERVISOR"
    );

    private final AccountSessionVerifier accounts;

    private final SecurityContextLogoutHandler logout =
            new SecurityContextLogoutHandler();

    private final CookieClearingLogoutHandler cookies =
            new CookieClearingLogoutHandler("WBSESSION");

    public AccountSessionFilter(
            AccountSessionVerifier accounts,
            SecurityContextRepository contexts) {

        this.accounts = Objects.requireNonNull(accounts);

        logout.setSecurityContextRepository(
                Objects.requireNonNull(contexts)
        );
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain chain)
            throws ServletException, IOException {

        Authentication authentication =
                SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null
                || !authentication.isAuthenticated()
                || authentication instanceof AnonymousAuthenticationToken) {

            chain.doFilter(request, response);
            return;
        }

        if (!(authentication.getPrincipal()
                instanceof WorkbenchPrincipal principal)
                || !hasExpectedAuthority(authentication, principal)) {

            rejectSession(request, response, authentication);
            return;
        }

        boolean currentAccount;

        try {
            currentAccount = accounts.isCurrent(principal);
        } catch (DataAccessException exception) {
            response.sendError(
                    HttpServletResponse.SC_SERVICE_UNAVAILABLE,
                    "Account verification is temporarily unavailable."
            );
            return;
        }

        if (!currentAccount) {
            rejectSession(request, response, authentication);
            return;
        }

        chain.doFilter(request, response);
    }

    private boolean hasExpectedAuthority(
            Authentication authentication,
            WorkbenchPrincipal principal) {

        String role = principal.role();

        if (role == null || !ALLOWED_ROLES.contains(role)) {
            return false;
        }

        if (authentication.getAuthorities().size() != 1) {
            return false;
        }

        String authority = authentication.getAuthorities()
                .iterator()
                .next()
                .getAuthority();

        return ("ROLE_" + role).equals(authority);
    }

    private void rejectSession(
            HttpServletRequest request,
            HttpServletResponse response,
            Authentication authentication) throws IOException {

        logout.logout(request, response, authentication);
        cookies.logout(request, response, authentication);

        response.sendError(
                HttpServletResponse.SC_UNAUTHORIZED,
                "Your session has ended. Please sign in again."
        );
    }
}