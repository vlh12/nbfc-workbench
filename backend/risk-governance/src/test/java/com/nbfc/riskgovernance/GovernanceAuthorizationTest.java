package com.nbfc.riskgovernance;

import com.nbfc.shared.ApiSecurity;
import com.nbfc.shared.auth.AccountSessionVerifier;
import com.nbfc.shared.auth.WorkbenchPrincipal;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(
        controllers = GovernanceController.class,
        properties = "WORKBENCH_DEMO_ENABLED=true"
)
@Import(ApiSecurity.class)
class GovernanceAuthorizationTest {

    private static final String USER_ID =
            "00000000-0000-0000-0000-000000000042";

    @Autowired
    MockMvc mvc;

    @MockitoBean
    GovernanceService service;

    @MockitoBean
    AssessmentRepository assessments;

    @MockitoBean
    FollowUpRepository followUps;

    @MockitoBean
    AuditRepository audit;

    @MockitoBean
    AccountSessionVerifier accountSessions;

    @BeforeEach
    void currentAccount() {
        when(accountSessions.isCurrent(any(WorkbenchPrincipal.class)))
                .thenReturn(true);
    }

    @Test
    void contributorCannotOpenSupervisorQueue() throws Exception {
        mvc.perform(
                get("/api/governance/assessments/queue")
                        .with(signedIn("CONTRIBUTOR"))
        ).andExpect(status().isForbidden());

        verifyNoInteractions(service);

        verify(audit).save(
                argThat(event ->
                        "UNAUTHORIZED_ATTEMPT".equals(event.action)
                )
        );
    }

    @Test
    void analystCannotApproveAssessment() throws Exception {
        mvc.perform(
                post("/api/governance/assessments/assessment-1/approve")
                        .contentType("application/json")
                        .content("{\"note\":\"No\"}")
                        .with(signedIn("ANALYST"))
                        .with(csrf())
        ).andExpect(status().isForbidden());

        verifyNoInteractions(service);
    }

    @Test
    void supervisorCannotSubmitAssessment() throws Exception {
        mvc.perform(
                post("/api/governance/assessments")
                        .contentType("application/json")
                        .content(
                                "{\"batchId\":\"synthetic-batch\","
                                + "\"note\":\"Review\"}"
                        )
                        .with(signedIn("SUPERVISOR"))
                        .with(csrf())
        ).andExpect(status().isForbidden());

        verifyNoInteractions(service);
    }

    private static RequestPostProcessor signedIn(String role) {
        var principal = new WorkbenchPrincipal(
                USER_ID,
                "test-user",
                role,
                0L
        );

        return authentication(
                UsernamePasswordAuthenticationToken.authenticated(
                        principal,
                        null,
                        List.of(
                                new SimpleGrantedAuthority("ROLE_" + role)
                        )
                )
        );
    }
}