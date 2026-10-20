package com.nbfc.shared;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
@SpringBootTest(classes=SwaggerDisabledTest.DisabledApp.class,properties={"WORKBENCH_DEMO_ENABLED=false","springdoc.api-docs.enabled=false","springdoc.swagger-ui.enabled=false"})
@AutoConfigureMockMvc
class SwaggerDisabledTest {
    @Configuration(proxyBeanMethods=false) @EnableAutoConfiguration @Import(ApiSecurity.class)
    static class DisabledApp {}
    @Autowired MockMvc mvc;
    @Test void demoDisabledDoesNotExposeSwaggerOrGatewaySpecs()throws Exception {
        for(String path:new String[]{"/v3/api-docs","/swagger-ui/index.html","/openapi/access-data"})
            mvc.perform(get(path).header("X-Demo-Role","SUPERVISOR")).andExpect(status().isUnauthorized());
    }
}
