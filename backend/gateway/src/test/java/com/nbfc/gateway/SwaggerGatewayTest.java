package com.nbfc.gateway;
import com.fasterxml.jackson.databind.JsonNode;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;
import static org.assertj.core.api.Assertions.assertThat;
@SpringBootTest(webEnvironment=SpringBootTest.WebEnvironment.RANDOM_PORT,properties="WORKBENCH_DEMO_ENABLED=true")
class SwaggerGatewayTest {
    static final Map<String,HttpServer> services=new LinkedHashMap<>();
    static {
        for(String service:new String[]{"access-data","exposure-analytics","stress-simulation","risk-governance"}) {
            try {
                var server=HttpServer.create(new InetSocketAddress("127.0.0.1",0),0);
                server.createContext("/v3/api-docs",exchange->{
                    byte[] body=("{\"openapi\":\"3.0.1\",\"info\":{\"title\":\""+service+"\",\"version\":\"v1\"},\"servers\":[{\"url\":\"/\"}],\"paths\":{}}").getBytes(StandardCharsets.UTF_8);
                    exchange.getResponseHeaders().set("Content-Type","application/json");exchange.sendResponseHeaders(200,body.length);exchange.getResponseBody().write(body);exchange.close();
                });server.start();services.put(service,server);
            } catch(java.io.IOException e){throw new ExceptionInInitializerError(e);}
        }
    }
    @DynamicPropertySource static void urls(DynamicPropertyRegistry registry) {
        services.forEach((name,server)->registry.add(name.replace('-','_').toUpperCase(java.util.Locale.ROOT)+"_URL",()->"http://127.0.0.1:"+server.getAddress().getPort()));
    }
    @AfterAll static void close(){services.values().forEach(server->server.stop(0));}
    @Autowired TestRestTemplate http;
    @Test void gatewayUiListsFourServicesAndProxiesTheirSpecs() {
        assertThat(http.getForEntity("/swagger-ui/index.html",String.class).getStatusCode().value()).isEqualTo(200);
        JsonNode config=http.getForObject("/v3/api-docs/swagger-config",JsonNode.class);
        assertThat(config.path("urls").size()).isEqualTo(4);
        for(String name:services.keySet()) {
            JsonNode spec=http.getForObject("/openapi/"+name,JsonNode.class);
            assertThat(spec.path("info").path("title").asText()).isEqualTo(name);
            assertThat(spec.path("servers").get(0).path("url").asText()).isEqualTo("/");
        }
        assertThat(http.getForEntity("/api/portfolios",String.class).getStatusCode().value()).isEqualTo(401);
    }
}
