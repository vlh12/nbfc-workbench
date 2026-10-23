package com.nbfc.accessdata;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import com.nbfc.accessdata.access.LendingReferenceController;
import com.nbfc.accessdata.access.LendingReportController;
import com.nbfc.accessdata.access.PortfolioController;
import com.nbfc.accessdata.access.UploadController;
import com.nbfc.accessdata.access.UploadErrors;

import com.nbfc.accessdata.data.BatchRepository;
import com.nbfc.accessdata.data.UploadService;
import com.nbfc.accessdata.data.portfolio.PortfolioCsv;
import com.nbfc.accessdata.data.portfolio.PortfolioStore;
import com.nbfc.accessdata.data.reference.LendingReferenceService;
import com.nbfc.accessdata.data.report.LendingReportService;
import com.nbfc.accessdata.data.report.LendingReportValidationService;
import com.nbfc.shared.ApiSecurity;

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
import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(
        classes = SwaggerApiTest.DocumentationOnly.class,
        properties = "WORKBENCH_DEMO_ENABLED=true"
)
@AutoConfigureMockMvc
class SwaggerApiTest {

    @Configuration(proxyBeanMethods = false)
    @EnableAutoConfiguration(excludeName = {
            "org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration",
            "org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration",
            "org.springframework.boot.autoconfigure.data.jpa.JpaRepositoriesAutoConfiguration",
            "org.springframework.boot.autoconfigure.kafka.KafkaAutoConfiguration"
    })
    @Import({
            ApiSecurity.class,
            UploadController.class,
            PortfolioController.class,
            LendingReferenceController.class,
            LendingReportController.class,
            UploadErrors.class
    })
    static class DocumentationOnly {
    }

    @Autowired
    MockMvc mvc;

    @Autowired
    ObjectMapper json;

    @MockitoBean
    UploadService mockUploadService;

    @MockitoBean
    BatchRepository mockBatchRepository;

    @MockitoBean
    PortfolioCsv mockPortfolioCsv;

    @MockitoBean
    PortfolioStore mockPortfolioStore;

    @MockitoBean
    LendingReferenceService mockLendingReferenceService;

    @MockitoBean
    LendingReportValidationService mockLendingReportValidationService;

    @MockitoBean
    LendingReportService mockLendingReportService;

    @Test
    void realControllerDocumentationIncludesServicePathsAndSessionCookie()
            throws Exception {

        String body = mvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.info.title")
                        .value("Access & Data API"))
                .andExpect(jsonPath(
                        "$.components.securitySchemes.SessionCookie.name"
                ).value("WBSESSION"))
                .andExpect(jsonPath(
                        "$.components.securitySchemes.SessionCookie.in"
                ).value("cookie"))
                .andExpect(jsonPath("$.servers[0].url").value("/"))
                .andReturn()
                .getResponse()
                .getContentAsString();

        JsonNode paths = json.readTree(body).path("paths");

        assertThat(paths.has("/api/portfolios")).isTrue();
        assertThat(paths.has("/api/portfolios/validate")).isTrue();
        assertThat(paths.has("/api/portfolios/capital")).isTrue();
        assertThat(paths.has("/api/portfolios/quality/{id}")).isTrue();
        assertThat(paths.has("/api/portfolios/thresholds")).isTrue();
        assertThat(paths.has("/api/batches")).isTrue();
        assertThat(paths.has("/api/lending-references")).isTrue();
        assertThat(paths.has("/api/lending-reports")).isTrue();

        assertThat(paths.has("/actuator/health")).isFalse();
        assertThat(body).contains("Allowed roles:");
    }

    @Test
    void swaggerAssetsAreReadableWithoutMakingBusinessApisPublic()
            throws Exception {

        mvc.perform(get("/swagger-ui.html"))
                .andExpect(status().is3xxRedirection());

        mvc.perform(get("/swagger-ui/index.html"))
                .andExpect(status().isOk())
                .andExpect(content().string(
                        containsString("Swagger UI")
                ));

        mvc.perform(get("/swagger-ui/swagger-ui-bundle.js"))
                .andExpect(status().isOk());

        mvc.perform(get("/api/portfolios"))
                .andExpect(status().isUnauthorized());
    }
}