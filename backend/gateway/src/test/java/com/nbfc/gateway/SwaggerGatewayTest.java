package com.nbfc.gateway;

import com.fasterxml.jackson.databind.JsonNode;
import com.sun.net.httpserver.HttpServer;

import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = {
                "WORKBENCH_DEMO_ENABLED=true",
                "spring.autoconfigure.exclude="
                        + "org.springframework.boot.autoconfigure.jdbc."
                        + "DataSourceAutoConfiguration"
        }
)
class SwaggerGatewayTest {

    static final Map<String, HttpServer> services =
            new LinkedHashMap<>();

    static {
        for (String service : new String[]{
                "access-data",
                "exposure-analytics",
                "stress-simulation",
                "risk-governance"
        }) {
            try {
                var server = HttpServer.create(
                        new InetSocketAddress("127.0.0.1", 0),
                        0
                );

                server.createContext("/v3/api-docs", exchange -> {
                    String specification = """
                            {
                              "openapi": "3.0.1",
                              "info": {
                                "title": "%s",
                                "version": "v1"
                              },
                              "servers": [
                                {"url": "/"}
                              ],
                              "paths": {}
                            }
                            """.formatted(service);

                    byte[] body = specification.getBytes(
                            StandardCharsets.UTF_8
                    );

                    exchange.getResponseHeaders().set(
                            "Content-Type",
                            "application/json"
                    );

                    exchange.sendResponseHeaders(200, body.length);
                    exchange.getResponseBody().write(body);
                    exchange.close();
                });

                if ("access-data".equals(service)) {
                    server.createContext(
                            "/api/portfolios",
                            exchange -> {
                                exchange.getResponseHeaders().set(
                                        "X-Test-Upstream",
                                        "access-data"
                                );

                                exchange.sendResponseHeaders(401, -1);
                                exchange.close();
                            }
                    );
                }

                server.start();
                services.put(service, server);

            } catch (java.io.IOException exception) {
                throw new ExceptionInInitializerError(exception);
            }
        }
    }

    @DynamicPropertySource
    static void urls(DynamicPropertyRegistry registry) {
        services.forEach((name, server) -> {
            String property = name.replace('-', '_')
                    .toUpperCase(Locale.ROOT) + "_URL";

            registry.add(
                    property,
                    () -> "http://127.0.0.1:"
                            + server.getAddress().getPort()
            );
        });
    }

    @AfterAll
    static void close() {
        services.values().forEach(server -> server.stop(0));
    }

    @Autowired
    TestRestTemplate http;

    @Test
    void gatewayUiListsFourServicesAndProxiesTheirSpecs() {
        var ui = http.getForEntity(
                "/swagger-ui/index.html",
                String.class
        );

        assertThat(ui.getStatusCode().value()).isEqualTo(200);

        JsonNode config = http.getForObject(
                "/v3/api-docs/swagger-config",
                JsonNode.class
        );

        assertThat(config).isNotNull();
        assertThat(config.path("urls").size()).isEqualTo(4);

        for (String name : services.keySet()) {
            JsonNode specification = http.getForObject(
                    "/openapi/" + name,
                    JsonNode.class
            );

            assertThat(specification).isNotNull();

            assertThat(
                    specification.path("info").path("title").asText()
            ).isEqualTo(name);

            assertThat(
                    specification.path("servers")
                            .get(0)
                            .path("url")
                            .asText()
            ).isEqualTo("/");
        }

        var portfolios = http.getForEntity(
                "/api/portfolios",
                String.class
        );

        assertThat(
                portfolios.getStatusCode().value()
        ).isEqualTo(401);

        assertThat(
                portfolios.getHeaders().getFirst("X-Test-Upstream")
        ).isEqualTo("access-data");
    }
}