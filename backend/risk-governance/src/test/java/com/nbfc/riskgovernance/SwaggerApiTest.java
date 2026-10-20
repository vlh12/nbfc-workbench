package com.nbfc.riskgovernance;
import com.nbfc.shared.ApiSecurity;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
@SpringBootTest(classes=SwaggerApiTest.DocumentationOnly.class,properties="WORKBENCH_DEMO_ENABLED=true")
@AutoConfigureMockMvc
class SwaggerApiTest {
    @Configuration(proxyBeanMethods=false)
    @EnableAutoConfiguration(excludeName={"org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration","org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration","org.springframework.boot.autoconfigure.data.jpa.JpaRepositoriesAutoConfiguration","org.springframework.boot.autoconfigure.kafka.KafkaAutoConfiguration"})
    @Import({ApiSecurity.class, GovernanceController.class, ScenarioAssessmentController.class, GovernanceErrors.class})
    static class DocumentationOnly {}
    @Autowired MockMvc mvc;
    @Autowired ObjectMapper json;
    @MockitoBean GovernanceService mockGovernanceService;
    @MockitoBean AssessmentRepository mockAssessmentRepository;
    @MockitoBean FollowUpRepository mockFollowUpRepository;
    @MockitoBean AuditRepository mockAuditRepository;
    @Test void realControllerDocumentationIncludesServicePathsAndRoleHeader()throws Exception {
        String body=mvc.perform(get("/v3/api-docs")).andExpect(status().isOk())
            .andExpect(jsonPath("$.info.title").value("Risk Governance API"))
            .andExpect(jsonPath("$.components.securitySchemes.DemoRole.name").value("X-Demo-Role"))
            .andExpect(jsonPath("$.servers[0].url").value("/"))
            .andReturn().getResponse().getContentAsString();
        JsonNode paths=json.readTree(body).path("paths");
        assertThat(paths.has("/api/governance/assessments")).as("/api/governance/assessments").isTrue();
        assertThat(paths.has("/api/governance/scenario-assessments")).as("/api/governance/scenario-assessments").isTrue();
        assertThat(paths.has("/api/governance/follow-ups/{id}")).as("/api/governance/follow-ups/{id}").isTrue();
        assertThat(paths.has("/api/governance/audit")).as("/api/governance/audit").isTrue();
        assertThat(paths.has("/actuator/health")).isFalse();
        assertThat(body).contains("Allowed demo roles:");
    }
    @Test void swaggerAssetsAreReadableWithoutMakingBusinessApisPublic()throws Exception {
        mvc.perform(get("/swagger-ui.html")).andExpect(status().is3xxRedirection());
        mvc.perform(get("/swagger-ui/index.html")).andExpect(status().isOk()).andExpect(content().string(org.hamcrest.Matchers.containsString("Swagger UI")));
        mvc.perform(get("/swagger-ui/swagger-ui-bundle.js")).andExpect(status().isOk());
        mvc.perform(get("/api/governance/assessments")).andExpect(status().isUnauthorized());
    }
}
