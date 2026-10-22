package com.nbfc.accessdata.access;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nbfc.accessdata.data.portfolio.PortfolioCsv;
import com.nbfc.accessdata.data.portfolio.PortfolioStore;
import com.nbfc.shared.ApiSecurity;
import com.nbfc.shared.PortfolioData.Snapshot;
import com.nbfc.shared.auth.AccountSessionVerifier;
import com.nbfc.shared.auth.WorkbenchPrincipal;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

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
        controllers = PortfolioController.class,
        properties = "WORKBENCH_DEMO_ENABLED=false"
)
@Import(ApiSecurity.class)
class PortfolioAuthorizationTest {

    private static final String USER_ID =
            "00000000-0000-0000-0000-000000000042";

    @Autowired
    ObjectMapper json;

    @Autowired
    MockMvc mvc;

    @MockitoBean
    PortfolioStore store;

    @MockitoBean
    PortfolioCsv csv;

    @MockitoBean
    AccountSessionVerifier accountSessions;

    @BeforeEach
    void currentAccount() {
        when(accountSessions.isCurrent(any(WorkbenchPrincipal.class)))
                .thenReturn(true);
    }

    @Test
    void analystAndSupervisorReadSavedMetadata() throws Exception {
        mvc.perform(
                get("/api/portfolios")
                        .with(signedIn("ANALYST"))
        ).andExpect(status().isOk());

        mvc.perform(
                get("/api/portfolios")
                        .with(signedIn("SUPERVISOR"))
        ).andExpect(status().isOk());

        verify(store, times(2)).list();
    }

    @Test
    void analystCannotUpload() throws Exception {
        mvc.perform(
                multipart("/api/portfolios")
                        .file("file", "data".getBytes())
                        .param("synthetic", "true")
                        .with(signedIn("ANALYST"))
                        .with(csrf())
        ).andExpect(status().isForbidden());

        verifyNoInteractions(store, csv);
    }

    @Test
    void contributorCannotSetThresholds() throws Exception {
        mvc.perform(
                post("/api/portfolios/thresholds")
                        .contentType("application/json")
                        .content("{}")
                        .with(signedIn("CONTRIBUTOR"))
                        .with(csrf())
        ).andExpect(status().isForbidden());

        verifyNoInteractions(store);
    }

    @Test
    void anonymousCannotRead() throws Exception {
        mvc.perform(get("/api/portfolios"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void contributorNameAndGeneratedIdAreReturnedForMultipartAndJson()
            throws Exception {

        byte[] bytes;

        try (var input = getClass().getResourceAsStream(
                "/portfolio-16.csv")) {

            if (input == null) {
                throw new IllegalStateException(
                        "Test resource portfolio-16.csv was not found."
                );
            }

            bytes = input.readAllBytes();
        }

        var validation = new PortfolioCsv().validate(bytes);

        String id = UUID.randomUUID().toString();
        String name = "September Exposure Portfolio";

        var snapshot = new Snapshot(
                id,
                "SYN-BANK-001",
                LocalDate.parse("2026-09-30"),
                "VALIDATED",
                USER_ID,
                Instant.now(),
                validation.rows(),
                validation.warnings(),
                name
        );

        when(csv.validate(any(byte[].class)))
                .thenReturn(validation);

        when(store.save(
                eq(validation),
                eq(USER_ID),
                eq(name)
        )).thenReturn(snapshot);

        mvc.perform(
                multipart("/api/portfolios")
                        .file("file", bytes)
                        .param("synthetic", "true")
                        .param("name", name)
                        .with(signedIn("CONTRIBUTOR"))
                        .with(csrf())
        )
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(id))
                .andExpect(jsonPath("$.name").value(name));

        var body = new PortfolioController.JsonSubmission(
                validation.rows(),
                true,
                name
        );

        mvc.perform(
                post("/api/portfolios")
                        .contentType("application/json")
                        .content(json.writeValueAsString(body))
                        .with(signedIn("CONTRIBUTOR"))
                        .with(csrf())
        )
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(id))
                .andExpect(jsonPath("$.name").value(name));

        verify(store, times(2)).save(
                eq(validation),
                eq(USER_ID),
                eq(name)
        );
    }

    @Test
    void supervisorCannotChangeThresholdsWithoutCsrf()
            throws Exception {

        mvc.perform(
                post("/api/portfolios/thresholds")
                        .contentType("application/json")
                        .content("{}")
                        .with(signedIn("SUPERVISOR"))
        ).andExpect(status().isForbidden());

        verifyNoInteractions(store);
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