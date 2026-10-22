package com.nbfc.accessdata.auth;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nbfc.accessdata.auth.LoginChallengeService.VerifiedLogin;
import com.nbfc.shared.auth.AccountSessionVerifier;
import com.nbfc.shared.auth.WorkbenchPrincipal;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.web.server.ResponseStatusException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = PasswordChangeController.class)
@ContextConfiguration(classes = {
        PasswordChangeController.class,
        AuthenticationSecurityConfiguration.class
})
class PasswordChangeControllerTest {

    private static final String USER_ID =
            "00000000-0000-0000-0000-000000000001";

    private static final String PROOF_KEY =
            "WB_PASSWORD_CHANGE_PROOF";

    private static final String CURRENT_PASSWORD =
            "SyntheticCurrentPassword2026!";

    private static final String NEW_PASSWORD =
            "SyntheticReplacementPassword2026!";

    @Autowired
    MockMvc mvc;

    @Autowired
    ObjectMapper json;

    @MockitoBean
    LoginChallengeService credentials;

    @MockitoBean
    PasswordChangeService passwordChanges;

    @MockitoBean
    AccountSessionVerifier accounts;

    @BeforeEach
    void accountsAreCurrent() {
        when(accounts.isCurrent(any(WorkbenchPrincipal.class)))
                .thenReturn(true);
    }

    private VerifiedLogin account(String role) {
        return new VerifiedLogin(
                USER_ID,
                "test-user",
                role,
                7L
        );
    }

    private RequestPostProcessor signedIn(String role) {
        var principal = new WorkbenchPrincipal(
                USER_ID,
                "test-user",
                role,
                7L
        );

        var token = UsernamePasswordAuthenticationToken.authenticated(
                principal,
                null,
                List.of(
                        new SimpleGrantedAuthority("ROLE_" + role)
                )
        );

        return authentication(token);
    }

    private MockHttpServletRequestBuilder checkRequest(
            String username,
            String password,
            String role) throws Exception {

        return post("/api/auth/password/check")
                .contentType(MediaType.APPLICATION_JSON)
                .content(json.writeValueAsString(Map.of(
                        "username", username,
                        "currentPassword", password,
                        "role", role
                )));
    }

    private MockHttpServletRequestBuilder changeRequest(
            String flowId,
            String password,
            String confirmation) throws Exception {

        return post("/api/auth/password/change")
                .contentType(MediaType.APPLICATION_JSON)
                .content(json.writeValueAsString(Map.of(
                        "flowId", flowId,
                        "newPassword", password,
                        "confirmNewPassword", confirmation
                )));
    }

    private Flow start(String role) throws Exception {
        return start(account(role), new MockHttpSession());
    }

    private Flow start(
            VerifiedLogin account,
            MockHttpSession session) throws Exception {

        when(credentials.checkCredentials(
                account.username(),
                CURRENT_PASSWORD,
                ApplicationRole.valueOf(account.role())
        )).thenReturn(account);

        String previousSessionId = session.getId();

        String response = mvc.perform(
                checkRequest(
                        account.username(),
                        CURRENT_PASSWORD,
                        account.role()
                )
                        .session(session)
                        .with(csrf())
        )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username")
                        .value(account.username()))
                .andExpect(jsonPath("$.role")
                        .value(account.role()))
                .andExpect(jsonPath("$.expiresInSeconds").value(300))
                .andReturn()
                .getResponse()
                .getContentAsString();

        String flowId = json.readTree(response)
                .path("flowId")
                .asText();

        assertThat(flowId).isNotBlank();
        assertThat(session.getId()).isNotEqualTo(previousSessionId);
        assertThat(response).doesNotContain(CURRENT_PASSWORD);

        Object storedProof = session.getAttribute(PROOF_KEY);

        assertThat(storedProof).isInstanceOf(String.class);
        assertThat((String) storedProof)
                .doesNotContain(CURRENT_PASSWORD);

        return new Flow(session, flowId, account);
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "CONTRIBUTOR",
            "ANALYST",
            "SUPERVISOR"
    })
    void everyRoleCanCompletePasswordChange(String role) throws Exception {
        Flow flow = start(role);

        // Checking credentials must not authenticate the browser.
        mvc.perform(
                get("/api/auth/me").session(flow.session())
        ).andExpect(status().isUnauthorized());

        mvc.perform(
                changeRequest(
                        flow.id(),
                        NEW_PASSWORD,
                        NEW_PASSWORD
                )
                        .session(flow.session())
                        .with(csrf())
        )
                .andExpect(status().isNoContent())
                .andExpect(cookie().maxAge("WBSESSION", 0));

        assertThat(flow.session().isInvalid()).isTrue();

        var expiry = ArgumentCaptor.forClass(Long.class);

        verify(passwordChanges).changePassword(
                eq(flow.account()),
                expiry.capture(),
                eq(NEW_PASSWORD),
                eq(NEW_PASSWORD)
        );

        assertThat(expiry.getValue())
                .isGreaterThan(System.currentTimeMillis())
                .isLessThanOrEqualTo(
                        System.currentTimeMillis() + 300_000L
                );

        // The old flow cannot be used again in a fresh session.
        mvc.perform(
                changeRequest(
                        flow.id(),
                        NEW_PASSWORD,
                        NEW_PASSWORD
                ).with(csrf())
        ).andExpect(status().isUnauthorized());

        verify(passwordChanges, times(1)).changePassword(
                any(VerifiedLogin.class),
                anyLong(),
                anyString(),
                anyString()
        );
    }

    @Test
    void badCredentialsDoNotIssueProof() throws Exception {
        when(credentials.checkCredentials(
                "test-user",
                "incorrect",
                ApplicationRole.ANALYST
        )).thenThrow(new BadCredentialsException("Invalid credentials"));

        var session = new MockHttpSession();

        mvc.perform(
                checkRequest("test-user", "incorrect", "ANALYST")
                        .session(session)
                        .with(csrf())
        )
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.restartRequired").value(false));

        assertThat(session.getAttribute(PROOF_KEY)).isNull();
        verifyNoInteractions(passwordChanges);
    }

    @Test
    void credentialCheckRequiresCsrf() throws Exception {
        mvc.perform(
                checkRequest(
                        "test-user",
                        CURRENT_PASSWORD,
                        "ANALYST"
                )
        ).andExpect(status().isForbidden());

        verifyNoInteractions(credentials, passwordChanges);
    }

    @Test
    void passwordChangeRequiresCsrf() throws Exception {
        Flow flow = start("ANALYST");

        mvc.perform(
                changeRequest(
                        flow.id(),
                        NEW_PASSWORD,
                        NEW_PASSWORD
                ).session(flow.session())
        ).andExpect(status().isForbidden());

        verifyNoInteractions(passwordChanges);
        assertThat(flow.session().isInvalid()).isFalse();
    }

    @Test
    void passwordChangeRequiresProof() throws Exception {
        mvc.perform(
                changeRequest(
                        UUID.randomUUID().toString(),
                        NEW_PASSWORD,
                        NEW_PASSWORD
                ).with(csrf())
        )
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.restartRequired").value(true));

        verifyNoInteractions(passwordChanges);
    }

    @Test
    void proofCannotBeUsedFromAnotherBrowserSession() throws Exception {
        Flow flow = start("ANALYST");

        mvc.perform(
                changeRequest(
                        flow.id(),
                        NEW_PASSWORD,
                        NEW_PASSWORD
                )
                        .session(new MockHttpSession())
                        .with(csrf())
        ).andExpect(status().isUnauthorized());

        verifyNoInteractions(passwordChanges);
        assertThat(flow.session().isInvalid()).isFalse();
    }

    @Test
    void newerCheckSupersedesOlderFlow() throws Exception {
        var session = new MockHttpSession();

        Flow older = start(account("ANALYST"), session);

        var anotherAccount = new VerifiedLogin(
                "00000000-0000-0000-0000-000000000002",
                "another-user",
                "CONTRIBUTOR",
                2L
        );

        Flow newer = start(anotherAccount, session);

        mvc.perform(
                changeRequest(
                        older.id(),
                        NEW_PASSWORD,
                        NEW_PASSWORD
                )
                        .session(session)
                        .with(csrf())
        ).andExpect(status().isUnauthorized());

        verifyNoInteractions(passwordChanges);
        assertThat(session.isInvalid()).isFalse();

        mvc.perform(
                changeRequest(
                        newer.id(),
                        NEW_PASSWORD,
                        NEW_PASSWORD
                )
                        .session(session)
                        .with(csrf())
        ).andExpect(status().isNoContent());

        verify(passwordChanges).changePassword(
                eq(anotherAccount),
                anyLong(),
                eq(NEW_PASSWORD),
                eq(NEW_PASSWORD)
        );
    }

    @Test
    void expiredProofIsRejected() throws Exception {
        Flow flow = start("ANALYST");

        String stored = (String) flow.session()
                .getAttribute(PROOF_KEY);

        var proof = json.readValue(
                stored,
                PasswordChangeController.PendingChange.class
        );

        var expired = new PasswordChangeController.PendingChange(
                proof.flowId(),
                proof.account(),
                System.currentTimeMillis() - 1000L
        );

        flow.session().setAttribute(
                PROOF_KEY,
                json.writeValueAsString(expired)
        );

        mvc.perform(
                changeRequest(
                        flow.id(),
                        NEW_PASSWORD,
                        NEW_PASSWORD
                )
                        .session(flow.session())
                        .with(csrf())
        )
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.restartRequired").value(true));

        verifyNoInteractions(passwordChanges);
    }

    @Test
    void confirmationMismatchKeepsVerificationAvailable() throws Exception {
        Flow flow = start("ANALYST");

        mvc.perform(
                changeRequest(
                        flow.id(),
                        NEW_PASSWORD,
                        NEW_PASSWORD + "different"
                )
                        .session(flow.session())
                        .with(csrf())
        )
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.restartRequired").value(false));

        assertThat(flow.session().isInvalid()).isFalse();
        verifyNoInteractions(passwordChanges);

        mvc.perform(
                changeRequest(
                        flow.id(),
                        NEW_PASSWORD,
                        NEW_PASSWORD
                )
                        .session(flow.session())
                        .with(csrf())
        ).andExpect(status().isNoContent());
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "CONTRIBUTOR",
            "ANALYST",
            "SUPERVISOR"
    })
    void signedInUsersMustLogOutFirst(String role) throws Exception {
        mvc.perform(
                checkRequest(
                        "test-user",
                        CURRENT_PASSWORD,
                        role
                )
                        .with(signedIn(role))
                        .with(csrf())
        ).andExpect(status().isConflict());

        mvc.perform(
                changeRequest(
                        UUID.randomUUID().toString(),
                        NEW_PASSWORD,
                        NEW_PASSWORD
                )
                        .with(signedIn(role))
                        .with(csrf())
        ).andExpect(status().isConflict());

        verifyNoInteractions(credentials, passwordChanges);
    }

    @Test
    void samePasswordFailureRequiresNewVerification() throws Exception {
        Flow flow = start("ANALYST");

        doThrow(new ResponseStatusException(
                HttpStatus.BAD_REQUEST,
                "Choose a password different from your current password."
        )).when(passwordChanges).changePassword(
                any(VerifiedLogin.class),
                anyLong(),
                anyString(),
                anyString()
        );

        mvc.perform(
                changeRequest(
                        flow.id(),
                        CURRENT_PASSWORD,
                        CURRENT_PASSWORD
                )
                        .session(flow.session())
                        .with(csrf())
        )
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.restartRequired").value(true));

        assertThat(flow.session().isInvalid()).isTrue();
    }

    @Test
    void concurrentAccountChangeReturnsConflict() throws Exception {
        Flow flow = start("ANALYST");

        doThrow(new OptimisticLockingFailureException(
                "Simulated concurrent account update"
        )).when(passwordChanges).changePassword(
                any(VerifiedLogin.class),
                anyLong(),
                anyString(),
                anyString()
        );

        mvc.perform(
                changeRequest(
                        flow.id(),
                        NEW_PASSWORD,
                        NEW_PASSWORD
                )
                        .session(flow.session())
                        .with(csrf())
        )
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.restartRequired").value(true));

        assertThat(flow.session().isInvalid()).isTrue();
    }

    @Test
    void databaseFailureReturnsAnUnconfirmedResult() throws Exception {
        Flow flow = start("ANALYST");

        doThrow(new DataAccessResourceFailureException(
                "Simulated database failure"
        )).when(passwordChanges).changePassword(
                any(VerifiedLogin.class),
                anyLong(),
                anyString(),
                anyString()
        );

        mvc.perform(
                changeRequest(
                        flow.id(),
                        NEW_PASSWORD,
                        NEW_PASSWORD
                )
                        .session(flow.session())
                        .with(csrf())
        )
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.restartRequired").value(true))
                .andExpect(jsonPath("$.message").value(
                        "Could not confirm the password change. "
                        + "Please try signing in before starting again."
                ));
    }

    private record Flow(
            MockHttpSession session,
            String id,
            VerifiedLogin account) {
    }
}