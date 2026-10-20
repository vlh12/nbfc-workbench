package com.nbfc.stresssimulation;
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
    @Import({ApiSecurity.class, ScenarioController.class})
    static class DocumentationOnly {}
    @Autowired MockMvc mvc;
    @Autowired ObjectMapper json;
    @MockitoBean ScenarioService mockScenarioService;
    @Test void realControllerDocumentationIncludesServicePathsAndRoleHeader()throws Exception {
        String body=mvc.perform(get("/v3/api-docs")).andExpect(status().isOk())
            .andExpect(jsonPath("$.info.title").value("Stress Simulation API"))
            .andExpect(jsonPath("$.components.securitySchemes.DemoRole.name").value("X-Demo-Role"))
            .andExpect(jsonPath("$.servers[0].url").value("/"))
            .andReturn().getResponse().getContentAsString();
        JsonNode paths=json.readTree(body).path("paths");
        assertThat(paths.has("/api/scenarios")).as("/api/scenarios").isTrue();
        assertThat(paths.has("/api/scenarios/{id}")).as("/api/scenarios/{id}").isTrue();
        assertThat(paths.has("/actuator/health")).isFalse();
        assertThat(body).contains("Allowed demo roles:");
    }
    @Test void swaggerAssetsAreReadableWithoutMakingBusinessApisPublic()throws Exception {
        mvc.perform(get("/swagger-ui.html")).andExpect(status().is3xxRedirection());
        mvc.perform(get("/swagger-ui/index.html")).andExpect(status().isOk()).andExpect(content().string(org.hamcrest.Matchers.containsString("Swagger UI")));
        mvc.perform(get("/swagger-ui/swagger-ui-bundle.js")).andExpect(status().isOk());
        mvc.perform(get("/api/scenarios")).andExpect(status().isUnauthorized());
    }
}
