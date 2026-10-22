package com.nbfc.gateway;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.ApplicationContext;
import org.springframework.session.web.http.SessionRepositoryFilter;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = {
                "WORKBENCH_DEMO_ENABLED=false",
                "spring.autoconfigure.exclude="
                        + "org.springframework.boot.autoconfigure.jdbc."
                        + "DataSourceAutoConfiguration"
        })
class GatewayAuthenticationRelayTest {

    private static final AtomicReference<String> receivedCookie =
            new AtomicReference<>();

    private static final AtomicReference<String> receivedCsrf =
            new AtomicReference<>();

    private static final AtomicReference<String> receivedDemoRole =
            new AtomicReference<>();

    private static final AtomicReference<String> receivedBody =
            new AtomicReference<>();

    private static final HttpServer upstream = startUpstream();

    @LocalServerPort
    int gatewayPort;

    @Autowired
    ApplicationContext applicationContext;

    @DynamicPropertySource
    static void upstreamAddress(DynamicPropertyRegistry properties) {
        properties.add(
                "ACCESS_DATA_URL",
                () -> "http://127.0.0.1:"
                        + upstream.getAddress().getPort());
    }

    @AfterAll
    static void stopUpstream() {
        upstream.stop(0);
    }

    @Test
    void forwardsAuthenticationHeadersBodyAndReplacementCookie()
            throws Exception {

        String body = """
                {"code":"K7P4M9"}
                """;

        var request = HttpRequest.newBuilder(
                        URI.create(baseUrl() + "/api/auth/verify"))
                .header("Content-Type", "application/json")
                .header("Cookie", "WBSESSION=test-pending-session")
                .header("X-CSRF-TOKEN", "test-csrf")
                .header("X-Demo-Role", "SUPERVISOR")
                .POST(HttpRequest.BodyPublishers.ofString(body))
                .build();

        var response = HttpClient.newHttpClient().send(
                request,
                HttpResponse.BodyHandlers.ofString());

        assertThat(response.statusCode()).isEqualTo(200);

        assertThat(receivedCookie.get())
                .contains("WBSESSION=test-pending-session");

        assertThat(receivedCsrf.get()).isEqualTo("test-csrf");
        assertThat(receivedDemoRole.get()).isNull();
        assertThat(receivedBody.get()).isEqualTo(body);

        assertThat(response.headers().allValues("Set-Cookie"))
                .contains(
                        "WBSESSION=test-authenticated-session; "
                                + "Path=/; HttpOnly");

        assertThat(applicationContext.getBeansOfType(
                SessionRepositoryFilter.class)).isEmpty();
    }

    @Test
    void preservesUnauthorizedResponseFromAccessData()
            throws Exception {

        var request = HttpRequest.newBuilder(
                        URI.create(baseUrl() + "/api/auth/me"))
                .GET()
                .build();

        var response = HttpClient.newHttpClient().send(
                request,
                HttpResponse.BodyHandlers.ofString());

        assertThat(response.statusCode()).isEqualTo(401);

        assertThat(response.headers()
                .firstValue("X-Test-Upstream"))
                .contains("access-data");
    }

    private String baseUrl() {
        return "http://127.0.0.1:" + gatewayPort;
    }

    private static HttpServer startUpstream() {
        try {
            var server = HttpServer.create(
                    new InetSocketAddress("127.0.0.1", 0),
                    0);

            server.createContext("/api/auth", exchange -> {
                exchange.getResponseHeaders().set(
                        "X-Test-Upstream",
                        "access-data");

                if (exchange.getRequestURI().getPath()
                        .equals("/api/auth/verify")) {

                    receivedCookie.set(
                            exchange.getRequestHeaders()
                                    .getFirst("Cookie"));

                    receivedCsrf.set(
                            exchange.getRequestHeaders()
                                    .getFirst("X-CSRF-TOKEN"));

                    receivedDemoRole.set(
                            exchange.getRequestHeaders()
                                    .getFirst("X-Demo-Role"));

                    receivedBody.set(new String(
                            exchange.getRequestBody().readAllBytes(),
                            StandardCharsets.UTF_8));

                    exchange.getResponseHeaders().add(
                            "Set-Cookie",
                            "WBSESSION=test-authenticated-session; "
                                    + "Path=/; HttpOnly");

                    reply(exchange, 200, "{\"role\":\"SUPERVISOR\"}");
                } else {
                    reply(exchange, 401, "{\"message\":\"Please sign in.\"}");
                }
            });

            server.start();
            return server;

        } catch (IOException exception) {
            throw new ExceptionInInitializerError(exception);
        }
    }

    private static void reply(
            HttpExchange exchange,
            int status,
            String body) throws IOException {

        byte[] bytes = body.getBytes(StandardCharsets.UTF_8);

        exchange.getResponseHeaders().set(
                "Content-Type",
                "application/json");

        exchange.sendResponseHeaders(status, bytes.length);

        try (var output = exchange.getResponseBody()) {
            output.write(bytes);
        } finally {
            exchange.close();
        }
    }
}