package com.nbfc.shared.auth;

import java.util.Objects;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.jdbc.core.JdbcTemplate;

public class AccountSessionVerifier {

    private static final String CHECK_ACCOUNT = """
            SELECT COUNT(*)
            FROM wb_app_user
            WHERE id = ?
              AND username = ?
              AND role_name = ?
              AND version = ?
              AND status = 'ACTIVE'
            """;

    private final ObjectProvider<JdbcTemplate> jdbcTemplates;

    public AccountSessionVerifier(
            ObjectProvider<JdbcTemplate> jdbcTemplates) {

        this.jdbcTemplates = Objects.requireNonNull(jdbcTemplates);
    }

    public boolean isCurrent(WorkbenchPrincipal principal) {
        if (principal == null
                || principal.userId() == null
                || principal.username() == null
                || principal.role() == null) {
            return false;
        }

        JdbcTemplate jdbc = jdbcTemplates.getIfAvailable();

        if (jdbc == null) {
            return false;
        }

        Integer matches = jdbc.queryForObject(
                CHECK_ACCOUNT,
                Integer.class,
                principal.userId(),
                principal.username(),
                principal.role(),
                principal.accountVersion()
        );

        return Integer.valueOf(1).equals(matches);
    }
}