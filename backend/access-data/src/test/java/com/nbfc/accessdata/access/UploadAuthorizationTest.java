package com.nbfc.accessdata.access;

import com.nbfc.accessdata.data.BatchRepository;
import com.nbfc.accessdata.data.UploadService;
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
        controllers = UploadController.class,
        properties = "WORKBENCH_DEMO_ENABLED=true"
)
@Import(ApiSecurity.class)
class UploadAuthorizationTest {

    private static final String USER_ID =
            "00000000-0000-0000-0000-000000000042";

    @Autowired
    MockMvc mvc;

    @MockitoBean
    UploadService service;

    @MockitoBean
    BatchRepository batches;

    @MockitoBean
    AccountSessionVerifier accountSessions;

    @BeforeEach
    void currentAccount() {
        when(accountSessions.isCurrent(any(WorkbenchPrincipal.class)))
                .thenReturn(true);
    }

    @Test
    void unauthenticatedCannotList() throws Exception {
        mvc.perform(get("/api/batches"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void analystCannotUpload() throws Exception {
        mvc.perform(
                multipart("/api/batches")
                        .file("file", "data".getBytes())
                        .param("synthetic", "true")
                        .with(signedIn("ANALYST"))
                        .with(csrf())
        ).andExpect(status().isForbidden());

        verifyNoInteractions(service);
    }

    @Test
    void unknownRoleCannotListBatches() throws Exception {
        mvc.perform(
                get("/api/batches")
                        .header("X-Demo-Role", "ADMIN")
        ).andExpect(status().isUnauthorized());

        verifyNoInteractions(batches);
    }

    @Test
    void contributorOnlyListsOwnBatches() throws Exception {
        mvc.perform(
                get("/api/batches")
                        .with(signedIn("CONTRIBUTOR"))
        ).andExpect(status().isOk());

        verify(batches)
                .findTop50BySubmittedByOrderByCreatedAtDesc(USER_ID);

        verify(batches, never())
                .findTop50ByOrderByCreatedAtDesc();
    }

    @Test
    void demoHeaderCannotAuthenticate() throws Exception {
        mvc.perform(
                get("/api/batches")
                        .header("X-Demo-Role", "CONTRIBUTOR")
        ).andExpect(status().isUnauthorized());

        verifyNoInteractions(batches);
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