package com.nbfc.accessdata.access;
import com.nbfc.accessdata.data.*;
import com.nbfc.shared.ApiSecurity;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.mockito.Mockito.*;
@WebMvcTest(controllers=UploadController.class,properties="WORKBENCH_DEMO_ENABLED=true")
@Import(ApiSecurity.class)
class UploadAuthorizationTest {
    @Autowired MockMvc mvc;
    @MockitoBean UploadService service;
    @MockitoBean BatchRepository batches;
    @Test void unauthenticatedCannotList() throws Exception {
        mvc.perform(get("/api/batches")).andExpect(status().isUnauthorized());
    }
    @Test void analystCannotUpload() throws Exception {
        mvc.perform(multipart("/api/batches").file("file","data".getBytes()).param("synthetic","true")
            .header("X-Demo-Role","ANALYST"))
            .andExpect(status().isForbidden());
        verifyNoInteractions(service);
    }
    @Test void unknownRoleCannotListBatches() throws Exception {
        mvc.perform(get("/api/batches").header("X-Demo-Role","ADMIN"))
            .andExpect(status().isUnauthorized());
        verifyNoInteractions(batches);
    }
    @Test void contributorOnlyListsOwnBatches() throws Exception {
        mvc.perform(get("/api/batches").header("X-Demo-Role","CONTRIBUTOR")).andExpect(status().isOk());
        verify(batches).findTop50BySubmittedByOrderByCreatedAtDesc("synthetic-contributor");
        verify(batches,never()).findTop50ByOrderByCreatedAtDesc();
    }
}
