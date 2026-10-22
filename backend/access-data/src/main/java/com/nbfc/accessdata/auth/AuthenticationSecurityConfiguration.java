package com.nbfc.accessdata.auth;

import java.util.List;

import com.nbfc.shared.auth.AccountSessionConfiguration;
import com.nbfc.shared.auth.AccountSessionFilter;
import com.nbfc.shared.auth.AccountSessionVerifier;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.session.CompositeSessionAuthenticationStrategy;
import org.springframework.security.web.authentication.session.SessionAuthenticationStrategy;
import org.springframework.security.web.authentication.session.SessionFixationProtectionStrategy;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.security.web.context.SecurityContextHolderFilter;
import org.springframework.security.web.csrf.CsrfAuthenticationStrategy;
import org.springframework.security.web.csrf.HttpSessionCsrfTokenRepository;

@Configuration(proxyBeanMethods = false)
@Import(AccountSessionConfiguration.class)
public class AuthenticationSecurityConfiguration {

    @Bean
    public HttpSessionSecurityContextRepository workbenchAuthContexts() {
        return new HttpSessionSecurityContextRepository();
    }

    @Bean
    public HttpSessionCsrfTokenRepository workbenchAuthCsrfTokens() {
        return new HttpSessionCsrfTokenRepository();
    }

    @Bean
    public SessionAuthenticationStrategy workbenchLoginSessionStrategy(
            HttpSessionCsrfTokenRepository csrfTokens) {

        var fixationProtection =
                new SessionFixationProtectionStrategy();

        fixationProtection.setMigrateSessionAttributes(false);

        return new CompositeSessionAuthenticationStrategy(List.of(
                fixationProtection,
                new CsrfAuthenticationStrategy(csrfTokens)
        ));
    }

    @Bean
    @Order(0)
    public SecurityFilterChain authenticationEndpoints(
            HttpSecurity http,
            HttpSessionSecurityContextRepository contexts,
            HttpSessionCsrfTokenRepository csrfTokens,
            AccountSessionVerifier accounts) throws Exception {

        http
                .securityMatcher("/api/auth/**")

                .securityContext(context -> context
                        .requireExplicitSave(true)
                        .securityContextRepository(contexts))

                .sessionManagement(session -> session
                        .sessionCreationPolicy(
                                SessionCreationPolicy.IF_REQUIRED))

                .csrf(csrf -> csrf
                        .csrfTokenRepository(csrfTokens))

                .requestCache(cache -> cache.disable())
                .formLogin(form -> form.disable())
                .httpBasic(basic -> basic.disable())
                .logout(logout -> logout.disable())

                .exceptionHandling(errors -> errors
                        .authenticationEntryPoint(
                                (request, response, error) ->
                                        response.sendError(401))
                        .accessDeniedHandler(
                                (request, response, error) ->
                                        response.sendError(403)))

                .authorizeHttpRequests(access -> access
                        .requestMatchers(
                                HttpMethod.GET,
                                "/api/auth/csrf")
                        .permitAll()

                        .requestMatchers(
                                HttpMethod.POST,
                                "/api/auth/login",
                                "/api/auth/verify",
                                "/api/auth/logout",
                                "/api/auth/password/check",
                                "/api/auth/password/change")
                        .permitAll()

                        .requestMatchers(
                                HttpMethod.GET,
                                "/api/auth/me")
                        .authenticated()

                        .requestMatchers(
                                HttpMethod.POST,
                                "/api/auth/users")
                        .hasRole("SUPERVISOR")

                        .anyRequest()
                        .denyAll());

        http.addFilterAfter(
                new AccountSessionFilter(accounts, contexts),
                SecurityContextHolderFilter.class
        );

        return http.build();
    }
}