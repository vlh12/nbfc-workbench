package com.nbfc.riskgovernance;
import com.nbfc.shared.ApiSecurity;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
@WebMvcTest(controllers=GovernanceController.class,properties="WORKBENCH_DEMO_ENABLED=true")
@Import(ApiSecurity.class)
class GovernanceAuthorizationTest {
    @Autowired MockMvc mvc;
    @MockitoBean GovernanceService service;
    @MockitoBean AssessmentRepository assessments;
    @MockitoBean FollowUpRepository followUps;
    @MockitoBean AuditRepository audit;
    @Test void contributorCannotOpenSupervisorQueue() throws Exception {
        mvc.perform(get("/api/governance/assessments/queue").header("X-Demo-Role","CONTRIBUTOR")).andExpect(status().isForbidden());
        verifyNoInteractions(service);
        verify(audit).save(argThat(e->e.action.equals("UNAUTHORIZED_ATTEMPT")));
    }
    @Test void analystCannotApproveAssessment() throws Exception {
        mvc.perform(post("/api/governance/assessments/assessment-1/approve").header("X-Demo-Role","ANALYST")
          .contentType("application/json").content("{\"note\":\"No\"}")).andExpect(status().isForbidden());
        verifyNoInteractions(service);
    }
    @Test void supervisorCannotSubmitAssessment() throws Exception {
        mvc.perform(post("/api/governance/assessments").header("X-Demo-Role","SUPERVISOR")
          .contentType("application/json").content("{\"title\":\"Review\",\"summary\":\"Details\"}")).andExpect(status().isForbidden());
        verifyNoInteractions(service);
    }
}
