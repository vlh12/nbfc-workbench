package com.nbfc.shared;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

import org.springdoc.core.customizers.OpenApiCustomizer;
import org.springdoc.core.customizers.OperationCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.core.env.Environment;
import org.springframework.security.access.prepost.PreAuthorize;

@Configuration(proxyBeanMethods = false)
public class OpenApiDocumentation {

    @Bean
    OpenAPI workbenchOpenApi(Environment environment) {
        String title = environment.getProperty(
                "workbench.openapi.title",
                "NBFC Workbench API"
        );

        return new OpenAPI()
                .info(new Info()
                        .title(title)
                        .version("v1")
                        .description(
                                "Synthetic NBFC workbench. Sign in through "
                                + "the workbench first. The browser supplies "
                                + "the WBSESSION cookie. Requests that change "
                                + "data also require the CSRF header returned "
                                + "by GET /api/auth/csrf."
                        )
                )
                .servers(List.of(
                        new Server()
                                .url("/")
                                .description(
                                        "Current service or API gateway"
                                )
                ))
                .components(new Components()
                        .addSecuritySchemes(
                                "SessionCookie",
                                new SecurityScheme()
                                        .type(SecurityScheme.Type.APIKEY)
                                        .in(SecurityScheme.In.COOKIE)
                                        .name("WBSESSION")
                                        .description(
                                                "Managed by the browser "
                                                + "after successful sign-in."
                                        )
                        )
                )
                .addSecurityItem(
                        new SecurityRequirement()
                                .addList("SessionCookie")
                );
    }

    @Bean
    OperationCustomizer roleDocumentation() {
        return (operation, handler) -> {
            PreAuthorize rule =
                    AnnotatedElementUtils.findMergedAnnotation(
                            handler.getMethod(),
                            PreAuthorize.class
                    );

            if (rule == null) {
                rule = AnnotatedElementUtils.findMergedAnnotation(
                        handler.getBeanType(),
                        PreAuthorize.class
                );
            }

            if (rule != null) {
                var matcher = Pattern.compile(
                        "'([A-Z_]+)'"
                ).matcher(rule.value());

                var roles = new ArrayList<String>();

                while (matcher.find()) {
                    roles.add(matcher.group(1));
                }

                if (!roles.isEmpty()) {
                    String existing = operation.getDescription();

                    operation.setDescription(
                            (existing == null ? "" : existing + "\n\n")
                                    + "Allowed roles: "
                                    + String.join(", ", roles)
                                    + "."
                    );
                }
            }

            return operation;
        };
    }

    @Bean
    OpenApiCustomizer publicAuthenticationDocumentation() {
        return api -> {
            if (api.getPaths() == null) {
                return;
            }

            for (String path : List.of(
                    "/api/auth/csrf",
                    "/api/auth/login",
                    "/api/auth/verify",
                    "/api/auth/logout"
            )) {
                var item = api.getPaths().get(path);

                if (item != null) {
                    item.readOperations().forEach(
                            operation ->
                                    operation.setSecurity(List.of())
                    );
                }
            }
        };
    }
}