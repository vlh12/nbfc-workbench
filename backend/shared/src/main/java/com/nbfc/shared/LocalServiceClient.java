package com.nbfc.shared;

import java.net.http.HttpClient;
import java.time.Duration;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;

public final class LocalServiceClient {
    private LocalServiceClient() {}
    public static <T> T get(String base, String path, Class<T> type) {
        var authentication = SecurityContextHolder.getContext().getAuthentication();
        String role = authentication == null ? "" : authentication.getAuthorities().stream()
            .map(a -> a.getAuthority().replace("ROLE_", "")).findFirst().orElse("");
        var http = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(3)).build();
        var factory = new JdkClientHttpRequestFactory(http);
        factory.setReadTimeout(Duration.ofSeconds(10));
        try {
            return RestClient.builder().baseUrl(base).requestFactory(factory).build()
                .get().uri(path).header("X-Demo-Role", role).retrieve().body(type);
        } catch (RestClientException error) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,
                "A required data service is unavailable or the referenced record is inaccessible.");
        }
    }
}
