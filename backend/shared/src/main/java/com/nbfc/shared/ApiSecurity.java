package com.nbfc.shared;

import com.nbfc.shared.auth.AccountSessionConfiguration;
import com.nbfc.shared.auth.AccountSessionFilter;
import com.nbfc.shared.auth.AccountSessionVerifier;

import jakarta.servlet.DispatcherType;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.security.web.context.SecurityContextHolderFilter;
import org.springframework.security.web.csrf.HttpSessionCsrfTokenRepository;

@Configuration
@EnableMethodSecurity
@Import({
        OpenApiDocumentation.class,
        AccountSessionConfiguration.class
})
public class ApiSecurity {

    private static final String[] DOCUMENTATION_PATHS = {
            "/swagger-ui.html",
            "/swagger-ui/**",
            "/v3/api-docs",
            "/v3/api-docs/**",
            "/v3/api-docs.yaml",
            "/openapi/**"
    };

    @Bean
    SecurityFilterChain security(
            HttpSecurity http,
            AccountSessionVerifier accounts,
            @Value("${WORKBENCH_DEMO_ENABLED:false}")
            boolean documentationEnabled) throws Exception {

        var contexts = new HttpSessionSecurityContextRepository();
        var csrfTokens = new HttpSessionCsrfTokenRepository();

        http
                .securityContext(context -> context
                        .securityContextRepository(contexts)
                        .requireExplicitSave(true)
                )
                .sessionManagement(session -> session
                        .sessionCreationPolicy(
                                SessionCreationPolicy.IF_REQUIRED
                        )
                )
                .csrf(csrf -> csrf
                        .csrfTokenRepository(csrfTokens)
                )
                .requestCache(cache -> cache.disable())
                .formLogin(form -> form.disable())
                .httpBasic(basic -> basic.disable())
                .logout(logout -> logout.disable())
                .exceptionHandling(errors -> errors
                        .authenticationEntryPoint(
                                (request, response, exception) ->
                                        response.sendError(401)
                        )
                        .accessDeniedHandler(
                                (request, response, exception) ->
                                        response.sendError(403)
                        )
                )
                .authorizeHttpRequests(authorize -> {
                    authorize.dispatcherTypeMatchers(
                            DispatcherType.ERROR
                    ).permitAll();

                    authorize.requestMatchers(
                            "/actuator/health"
                    ).permitAll();

                    if (documentationEnabled) {
                        authorize.requestMatchers(
                                HttpMethod.GET,
                                DOCUMENTATION_PATHS
                        ).permitAll();
                    } else {
                        authorize.requestMatchers(
                                DOCUMENTATION_PATHS
                        ).denyAll();
                    }

                    authorize.anyRequest().authenticated();
                });

        http.addFilterAfter(
                new AccountSessionFilter(accounts, contexts),
                SecurityContextHolderFilter.class
        );

        return http.build();
    }
}