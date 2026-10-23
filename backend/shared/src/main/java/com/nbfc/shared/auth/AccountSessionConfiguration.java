package com.nbfc.shared.auth;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;

@Configuration(proxyBeanMethods = false)
public class AccountSessionConfiguration {

    @Bean
    public AccountSessionVerifier accountSessionVerifier(
            ObjectProvider<JdbcTemplate> jdbcTemplates) {

        return new AccountSessionVerifier(jdbcTemplates);
    }
}