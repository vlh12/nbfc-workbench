package com.nbfc.accessdata.auth;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nbfc.shared.auth.AccountSessionVerifier;
import com.nbfc.shared.auth.WorkbenchPrincipal;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = UserManagementController.class)
@ContextConfiguration(classes = {
        UserManagementController.class,
        AuthenticationSecurityConfiguration.class,
        UserManagementControllerTest.TestSupport.class
})
class UserManagementControllerTest {

    private static final String SUPERVISOR_ID =
            "00000000-0000-0000-0000-000000000001";

    private static final String PASSWORD =
            "SyntheticInitialPassword2026!";

    @Autowired
    MockMvc mvc;

    @Autowired
    ObjectMapper json;

    @Autowired
    PasswordEncoder passwords;

    @MockitoBean
    ApplicationUserRepository users;

    @MockitoBean
    AccountSessionVerifier accounts;

    @TestConfiguration(proxyBeanMethods = false)
    @EnableMethodSecurity
    static class TestSupport {

        @Bean
        PasswordEncoder passwordEncoder() {
            return new BCryptPasswordEncoder(12);
        }
    }

    @BeforeEach
    void accountsAreCurrent() {
        when(accounts.isCurrent(any(WorkbenchPrincipal.class)))
                .thenReturn(true);
    }

    private RequestPostProcessor signedIn(String role) {
        var principal = new WorkbenchPrincipal(
                SUPERVISOR_ID,
                "test-account",
                role,
                0L
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

    private MockHttpServletRequestBuilder createRequest(
            String username,
            String password,
            String role) throws Exception {

        return post("/api/auth/users")
                .contentType(MediaType.APPLICATION_JSON)
                .content(json.writeValueAsString(Map.of(
                        "username", username,
                        "password", password,
                        "role", role
                )));
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "CONTRIBUTOR",
            "ANALYST",
            "SUPERVISOR"
    })
    void supervisorCanCreateEveryRole(String role) throws Exception {
        when(users.findByUsername("new.user"))
                .thenReturn(Optional.empty());

        when(users.saveAndFlush(any(ApplicationUser.class)))
                .thenAnswer(invocation ->
                        invocation.getArgument(
                                0,
                                ApplicationUser.class
                        ));

        String response = mvc.perform(
                createRequest("  New.User  ", PASSWORD, role)
                        .with(signedIn("SUPERVISOR"))
                        .with(csrf())
        )
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNotEmpty())
                .andExpect(jsonPath("$.username").value("new.user"))
                .andExpect(jsonPath("$.role").value(role))
                .andExpect(jsonPath("$.status").value("ACTIVE"))
                .andExpect(jsonPath("$.createdBy").value(SUPERVISOR_ID))
                .andExpect(jsonPath("$.createdAt").isNotEmpty())
                .andExpect(jsonPath("$.password").doesNotExist())
                .andExpect(jsonPath("$.passwordHash").doesNotExist())
                .andExpect(jsonPath("$.version").doesNotExist())
                .andReturn()
                .getResponse()
                .getContentAsString();

        var captured = ArgumentCaptor.forClass(ApplicationUser.class);
        verify(users).saveAndFlush(captured.capture());

        ApplicationUser saved = captured.getValue();

        assertThat(saved.getUsername()).isEqualTo("new.user");
        assertThat(saved.getRole())
                .isEqualTo(ApplicationRole.valueOf(role));
        assertThat(saved.getCreatedBy()).isEqualTo(SUPERVISOR_ID);

        assertThat(saved.getPasswordHash()).isNotEqualTo(PASSWORD);
        assertThat(
                passwords.matches(PASSWORD, saved.getPasswordHash())
        ).isTrue();

        assertThat(response)
                .doesNotContain(PASSWORD)
                .doesNotContain(saved.getPasswordHash());
    }

    @Test
    void demoHeaderCannotCreateUser() throws Exception {
        mvc.perform(
                createRequest("new.user", PASSWORD, "ANALYST")
                        .header("X-Demo-Role", "SUPERVISOR")
                        .with(csrf())
        ).andExpect(status().isUnauthorized());

        verifyNoInteractions(users);
    }

    @ParameterizedTest
    @ValueSource(strings = {"CONTRIBUTOR", "ANALYST"})
    void otherRolesCannotCreateUsers(String role) throws Exception {
        mvc.perform(
                createRequest("new.user", PASSWORD, "ANALYST")
                        .with(signedIn(role))
                        .with(csrf())
        ).andExpect(status().isForbidden());

        verifyNoInteractions(users);
    }

    @Test
    void supervisorNeedsCsrfToken() throws Exception {
        mvc.perform(
                createRequest("new.user", PASSWORD, "ANALYST")
                        .with(signedIn("SUPERVISOR"))
        ).andExpect(status().isForbidden());

        verifyNoInteractions(users);
    }

    @Test
    void existingUsernameReturnsConflict() throws Exception {
        var existing = new ApplicationUser(
                "analyst01",
                "unused-test-hash",
                ApplicationRole.ANALYST,
                SUPERVISOR_ID
        );

        when(users.findByUsername("analyst01"))
                .thenReturn(Optional.of(existing));

        mvc.perform(
                createRequest("  Analyst01  ", PASSWORD, "ANALYST")
                        .with(signedIn("SUPERVISOR"))
                        .with(csrf())
        )
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message")
                        .value("That username is already in use."));

        verify(users, never())
                .saveAndFlush(any(ApplicationUser.class));
    }

    @Test
    void duplicateInsertCollisionReturnsConflict() throws Exception {
        var existing = new ApplicationUser(
                "analyst01",
                "unused-test-hash",
                ApplicationRole.ANALYST,
                SUPERVISOR_ID
        );

        when(users.findByUsername("analyst01"))
                .thenReturn(Optional.empty())
                .thenReturn(Optional.of(existing));

        when(users.saveAndFlush(any(ApplicationUser.class)))
                .thenThrow(new DataIntegrityViolationException(
                        "Simulated duplicate username"
                ));

        mvc.perform(
                createRequest("analyst01", PASSWORD, "ANALYST")
                        .with(signedIn("SUPERVISOR"))
                        .with(csrf())
        )
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message")
                        .value("That username is already in use."));

        verify(users, times(1))
                .saveAndFlush(any(ApplicationUser.class));
    }

    @ParameterizedTest
    @ValueSource(strings = {"ab", "1analyst", "bad user"})
    void invalidUsernameIsRejected(String username) throws Exception {
        mvc.perform(
                createRequest(username, PASSWORD, "ANALYST")
                        .with(signedIn("SUPERVISOR"))
                        .with(csrf())
        ).andExpect(status().isBadRequest());

        verifyNoInteractions(users);
    }

    @ParameterizedTest
    @ValueSource(strings = {"short", "            "})
    void invalidPasswordIsRejected(String password) throws Exception {
        mvc.perform(
                createRequest("new.user", password, "ANALYST")
                        .with(signedIn("SUPERVISOR"))
                        .with(csrf())
        ).andExpect(status().isBadRequest());

        verifyNoInteractions(users);
    }

    @Test
    void passwordOverBcryptByteLimitIsRejected() throws Exception {
        String password = "\u20AC".repeat(25);

        mvc.perform(
                createRequest("new.user", password, "ANALYST")
                        .with(signedIn("SUPERVISOR"))
                        .with(csrf())
        ).andExpect(status().isBadRequest());

        verifyNoInteractions(users);
    }

    @ParameterizedTest
    @ValueSource(strings = {"MANAGER", ""})
    void invalidRoleIsRejected(String role) throws Exception {
        mvc.perform(
                createRequest("new.user", PASSWORD, role)
                        .with(signedIn("SUPERVISOR"))
                        .with(csrf())
        ).andExpect(status().isBadRequest());

        verifyNoInteractions(users);
    }
}