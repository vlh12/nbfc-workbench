package com.nbfc.shared.auth;

import java.util.List;

import jakarta.servlet.FilterChain;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

class AccountSessionFilterTest {

    private static final WorkbenchPrincipal USER =
            new WorkbenchPrincipal(
                    "00000000-0000-0000-0000-000000000042",
                    "supervisor01",
                    "SUPERVISOR",
                    0
            );

    private AccountSessionVerifier accounts;
    private AccountSessionFilter filter;

    @BeforeEach
    void setUp() {
        accounts = mock(AccountSessionVerifier.class);

        filter = new AccountSessionFilter(
                accounts,
                new HttpSessionSecurityContextRepository()
        );

        authenticate("ROLE_SUPERVISOR");
    }

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void currentAccountCanContinue() throws Exception {
        when(accounts.isCurrent(USER)).thenReturn(true);

        var request = signedInRequest();
        var response = new MockHttpServletResponse();
        var chain = mock(FilterChain.class);

        filter.doFilter(request, response, chain);

        verify(accounts).isCurrent(USER);
        verify(chain).doFilter(request, response);

        assertThat(response.getStatus()).isEqualTo(200);
        assertThat(request.getSession(false)).isNotNull();
    }

    @Test
    void staleAccountEndsSession() throws Exception {
        when(accounts.isCurrent(USER)).thenReturn(false);

        var request = signedInRequest();
        var oldSession = (MockHttpSession) request.getSession(false);
        var response = new MockHttpServletResponse();
        var chain = mock(FilterChain.class);

        filter.doFilter(request, response, chain);

        assertThat(response.getStatus()).isEqualTo(401);
        assertThat(oldSession.isInvalid()).isTrue();
        assertThat(request.getSession(false)).isNull();

        assertThat(
                SecurityContextHolder.getContext().getAuthentication()
        ).isNull();

        assertThat(response.getCookie("WBSESSION")).isNotNull();
        assertThat(
                response.getCookie("WBSESSION").getMaxAge()
        ).isZero();

        verifyNoInteractions(chain);
    }

    @Test
    void databaseFailurePreservesSession() throws Exception {
        when(accounts.isCurrent(USER)).thenThrow(
                new DataAccessResourceFailureException(
                        "Database unavailable"
                )
        );

        var authentication =
                SecurityContextHolder.getContext().getAuthentication();

        var request = signedInRequest();
        var oldSession = (MockHttpSession) request.getSession(false);
        var response = new MockHttpServletResponse();
        var chain = mock(FilterChain.class);

        filter.doFilter(request, response, chain);

        assertThat(response.getStatus()).isEqualTo(503);
        assertThat(oldSession.isInvalid()).isFalse();
        assertThat(request.getSession(false)).isSameAs(oldSession);

        assertThat(
                SecurityContextHolder.getContext().getAuthentication()
        ).isSameAs(authentication);

        assertThat(response.getCookie("WBSESSION")).isNull();

        verifyNoInteractions(chain);
    }

    @Test
    void mismatchedRoleEndsSession() throws Exception {
        authenticate("ROLE_ANALYST");

        var request = signedInRequest();
        var response = new MockHttpServletResponse();
        var chain = mock(FilterChain.class);

        filter.doFilter(request, response, chain);

        assertThat(response.getStatus()).isEqualTo(401);

        verifyNoInteractions(accounts, chain);
    }

    @Test
    void anonymousRequestContinuesWithoutAccountLookup() throws Exception {
        SecurityContextHolder.clearContext();

        var request = new MockHttpServletRequest(
                "GET",
                "/actuator/health"
        );

        var response = new MockHttpServletResponse();
        var chain = mock(FilterChain.class);

        filter.doFilter(request, response, chain);

        verify(chain).doFilter(request, response);
        verifyNoInteractions(accounts);
    }

    @Test
    void missingDatabaseSupportCannotValidateAnAccount() {
        var beans = new DefaultListableBeanFactory();

        var verifier = new AccountSessionVerifier(
                beans.getBeanProvider(JdbcTemplate.class)
        );

        assertThat(verifier.isCurrent(USER)).isFalse();
    }

    private static void authenticate(String authority) {
        var context = SecurityContextHolder.createEmptyContext();

        context.setAuthentication(
                UsernamePasswordAuthenticationToken.authenticated(
                        USER,
                        null,
                        List.of(new SimpleGrantedAuthority(authority))
                )
        );

        SecurityContextHolder.setContext(context);
    }

    private static MockHttpServletRequest signedInRequest() {
        var request = new MockHttpServletRequest(
                "GET",
                "/api/portfolios"
        );

        var session = new MockHttpSession();

        session.setAttribute(
                HttpSessionSecurityContextRepository
                        .SPRING_SECURITY_CONTEXT_KEY,
                SecurityContextHolder.getContext()
        );

        request.setSession(session);

        return request;
    }
}