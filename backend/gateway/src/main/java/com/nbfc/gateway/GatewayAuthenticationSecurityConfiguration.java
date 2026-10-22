package com.nbfc.gateway;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.context.NullSecurityContextRepository;

@Configuration(proxyBeanMethods = false)
public class GatewayAuthenticationSecurityConfiguration {

    @Bean
    @Order(0)
    public SecurityFilterChain authenticationRelay(
            HttpSecurity http) throws Exception {

        http
                .securityMatcher("/api/**")

                .securityContext(context -> context
                        .securityContextRepository(
                                new NullSecurityContextRepository()))

                .sessionManagement(session -> session
                        .sessionCreationPolicy(
                                SessionCreationPolicy.STATELESS))

                .requestCache(cache -> cache.disable())

                // Access & Data validates CSRF after forwarding.
                .csrf(csrf -> csrf.disable())

                .formLogin(form -> form.disable())
                .httpBasic(basic -> basic.disable())
                .logout(logout -> logout.disable())

                .authorizeHttpRequests(access -> access
                        .anyRequest().permitAll());

        return http.build();
    }
}