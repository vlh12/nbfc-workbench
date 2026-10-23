package com.nbfc.shared;

import com.nbfc.shared.auth.WorkbenchPrincipal;

import jakarta.servlet.http.Cookie;

import java.net.http.HttpClient;
import java.time.Duration;
import java.util.StringJoiner;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.springframework.web.server.ResponseStatusException;

public final class LocalServiceClient {

    private LocalServiceClient() {
    }

    public static <T> T get(
            String base,
            String path,
            Class<T> type) {

        String cookieHeader = sessionCookieHeader();

        var http = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(3))
                .build();

        var factory = new JdkClientHttpRequestFactory(http);
        factory.setReadTimeout(Duration.ofSeconds(10));

        try {
            return RestClient.builder()
                    .baseUrl(base)
                    .requestFactory(factory)
                    .build()
                    .get()
                    .uri(path)
                    .header(HttpHeaders.COOKIE, cookieHeader)
                    .retrieve()
                    .body(type);

        } catch (RestClientResponseException exception) {
            int status = exception.getStatusCode().value();

            if (status == 401 || status == 403) {
                throw new ResponseStatusException(
                        exception.getStatusCode(),
                        "A required service rejected your access."
                );
            }

            throw unavailable();

        } catch (RestClientException exception) {
            throw unavailable();
        }
    }

    private static String sessionCookieHeader() {
        var authentication =
                SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null
                || !authentication.isAuthenticated()
                || !(authentication.getPrincipal()
                        instanceof WorkbenchPrincipal)) {
            throw signInRequired();
        }

        if (!(RequestContextHolder.getRequestAttributes()
                instanceof ServletRequestAttributes attributes)) {
            throw signInRequired();
        }

        Cookie[] cookies = attributes.getRequest().getCookies();

        if (cookies == null) {
            throw signInRequired();
        }

        var header = new StringJoiner("; ");

        for (Cookie cookie : cookies) {
            if ("WBSESSION".equals(cookie.getName())
                    && cookie.getValue() != null
                    && !cookie.getValue().isBlank()) {

                header.add("WBSESSION=" + cookie.getValue());
            }
        }

        if (header.length() == 0) {
            throw signInRequired();
        }

        return header.toString();
    }

    private static ResponseStatusException signInRequired() {
        return new ResponseStatusException(
                HttpStatus.UNAUTHORIZED,
                "Please sign in again."
        );
    }

    private static ResponseStatusException unavailable() {
        return new ResponseStatusException(
                HttpStatus.SERVICE_UNAVAILABLE,
                "A required data service is unavailable "
                        + "or the referenced record is inaccessible."
        );
    }
}