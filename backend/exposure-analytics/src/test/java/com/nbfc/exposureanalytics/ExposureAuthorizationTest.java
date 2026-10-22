package com.nbfc.exposureanalytics;

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
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(
        controllers = ExposureController.class,
        properties = "WORKBENCH_DEMO_ENABLED=true"
)
@Import(ApiSecurity.class)
class ExposureAuthorizationTest {

    private static final String USER_ID =
            "00000000-0000-0000-0000-000000000042";

    @Autowired
    MockMvc mvc;

    @MockitoBean
    ExposureRepository repository;

    @MockitoBean
    AccountSessionVerifier accountSessions;

    @BeforeEach
    void currentAccount() {
        when(accountSessions.isCurrent(any(WorkbenchPrincipal.class)))
                .thenReturn(true);
    }

    @Test
    void contributorCannotReadAnalystSummary() throws Exception {
        mvc.perform(
                get("/api/exposures/summary")
                        .with(signedIn("CONTRIBUTOR"))
        ).andExpect(status().isForbidden());

        verifyNoInteractions(repository);
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