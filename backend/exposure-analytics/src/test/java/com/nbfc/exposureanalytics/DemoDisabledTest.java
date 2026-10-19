package com.nbfc.exposureanalytics;
import com.nbfc.shared.ApiSecurity;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
@WebMvcTest(controllers=ExposureController.class,properties="WORKBENCH_DEMO_ENABLED=false")
@Import(ApiSecurity.class)
class DemoDisabledTest {
    @Autowired MockMvc mvc;
    @MockitoBean ExposureRepository repository;
    @Test void demoHeaderCannotGrantAccessWhenDisabled() throws Exception {
        mvc.perform(get("/api/exposures/summary").header("X-Demo-Role","ANALYST"))
            .andExpect(status().isUnauthorized());
    }
}
