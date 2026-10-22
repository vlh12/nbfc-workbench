package com.nbfc.accessdata.auth;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nbfc.shared.ApiSecurity;

import java.util.List;

import com.nbfc.shared.auth.AccountSessionVerifier;
import com.nbfc.shared.auth.WorkbenchPrincipal;

import org.junit.jupiter.api.BeforeEach;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(
        controllers = AuthController.class,
        properties = "WORKBENCH_DEMO_ENABLED=false")
@Import({
        ApiSecurity.class,
        AuthenticationSecurityConfiguration.class
})
class AuthControllerSecurityTest {

    private static final String USER_ID =
            "00000000-0000-0000-0000-000000000042";

    private static final String LOGIN_BODY = """
            {
              "username": "supervisor01",
              "password": "synthetic-test-password",
              "role": "SUPERVISOR"
            }
            """;

    @Autowired
    MockMvc mvc;

    @Autowired
    ObjectMapper json;

    @MockitoBean
    LoginChallengeService challenges;

    @Test
    void demoRoleHeaderCannotAuthenticateProfile() throws Exception {
        mvc.perform(get("/api/auth/me")
                        .header("X-Demo-Role", "SUPERVISOR"))
                .andExpect(status().isUnauthorized());

        verifyNoInteractions(challenges);
    }

    @Test
    void loginRequiresCsrfProtection() throws Exception {
        mvc.perform(post("/api/auth/login")
                        .contentType(APPLICATION_JSON)
                        .content(LOGIN_BODY))
                .andExpect(status().isForbidden());

        verifyNoInteractions(challenges);
    }
    
    @MockitoBean
    AccountSessionVerifier accounts;

    @BeforeEach
    void allowCurrentAccountSessions() {
        when(accounts.isCurrent(any(WorkbenchPrincipal.class)))
                .thenReturn(true);
    }
    
    @Test
    void invalidAccountSessionCannotLoadProfile() throws Exception {
        when(accounts.isCurrent(any(WorkbenchPrincipal.class)))
                .thenReturn(false);

        var principal = new WorkbenchPrincipal(
                "00000000-0000-0000-0000-000000000042",
                "supervisor01",
                "SUPERVISOR",
                0
        );

        var token = UsernamePasswordAuthenticationToken.authenticated(
                principal,
                null,
                List.of(new SimpleGrantedAuthority("ROLE_SUPERVISOR"))
        );

        mvc.perform(
                get("/api/auth/me")
                        .with(authentication(token))
        ).andExpect(status().isUnauthorized());
    }

    @Test
    void loginVerificationCreatesSessionAndLogoutInvalidatesIt()
            throws Exception {

        when(challenges.begin(
                eq("supervisor01"),
                eq("synthetic-test-password"),
                eq(ApplicationRole.SUPERVISOR),
                anyString()))
                .thenReturn(new LoginChallengeService.IssuedCode(
                        "test-challenge",
                        "K7P4M9",
                        300));

        when(challenges.verify(
                eq("test-challenge"),
                eq("K7P4M9"),
                anyString()))
                .thenReturn(Optional.of(
                        new LoginChallengeService.VerifiedLogin(
                                USER_ID,
                                "supervisor01",
                                "SUPERVISOR",
                                0)));

        Browser pending = csrf(null);

        mvc.perform(postJson(
                        "/api/auth/login",
                        pending,
                        LOGIN_BODY))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("K7P4M9"));

        // Credentials alone must not establish authentication.
        mvc.perform(get("/api/auth/me")
                        .session(pending.session()))
                .andExpect(status().isUnauthorized());

        // A separate browser cannot verify this pending login.
        Browser stranger = csrf(null);

        mvc.perform(postJson(
                        "/api/auth/verify",
                        stranger,
                        """
                        {"code":"K7P4M9"}
                        """))
                .andExpect(status().isUnauthorized());

        String previousSessionId = pending.session().getId();

        var verification = mvc.perform(postJson(
                        "/api/auth/verify",
                        pending,
                        """
                        {"code":"K7P4M9"}
                        """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(USER_ID))
                .andExpect(jsonPath("$.role").value("SUPERVISOR"))
                .andReturn();

        MockHttpSession authenticatedSession =
                (MockHttpSession) verification
                        .getRequest()
                        .getSession(false);

        assertThat(authenticatedSession).isNotNull();
        assertThat(pending.session().isInvalid()).isTrue();

        assertThat(authenticatedSession.getId())
                .isNotEqualTo(previousSessionId);

        mvc.perform(get("/api/auth/me")
                        .session(authenticatedSession))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username")
                        .value("supervisor01"));

        // Authentication refreshes CSRF protection.
        Browser signedIn = csrf(authenticatedSession);

        mvc.perform(postJson(
                        "/api/auth/logout",
                        signedIn,
                        "{}"))
                .andExpect(status().isNoContent());

        assertThat(authenticatedSession.isInvalid()).isTrue();

        mvc.perform(get("/api/auth/me"))
                .andExpect(status().isUnauthorized());
    }

    private Browser csrf(MockHttpSession existing) throws Exception {
        var request = get("/api/auth/csrf");

        if (existing != null) {
            request.session(existing);
        }

        var result = mvc.perform(request)
                .andExpect(status().isOk())
                .andReturn();

        var body = json.readTree(
                result.getResponse().getContentAsString());

        return new Browser(
                (MockHttpSession) result
                        .getRequest()
                        .getSession(false),
                body.path("headerName").asText(),
                body.path("token").asText());
    }

    private MockHttpServletRequestBuilder postJson(
            String path,
            Browser browser,
            String body) {

        return post(path)
                .session(browser.session())
                .with(request -> {
                    request.setRequestedSessionId(
                            browser.session().getId());

                    request.setRequestedSessionIdValid(true);
                    request.setRequestedSessionIdFromCookie(true);

                    return request;
                })
                .header(browser.headerName(), browser.token())
                .contentType(APPLICATION_JSON)
                .content(body);
    }

    private record Browser(
            MockHttpSession session,
            String headerName,
            String token) {
    }
}