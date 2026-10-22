package com.nbfc.accessdata.auth;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.nio.charset.StandardCharsets;

@Configuration(proxyBeanMethods = false)
public class AccountSetupConfiguration {

    private static final Logger log =
        LoggerFactory.getLogger(AccountSetupConfiguration.class);

    @Bean
    public PasswordEncoder workbenchPasswordEncoder() {
        return new BCryptPasswordEncoder(12);
    }

    @Bean
    @ConditionalOnProperty(
        prefix = "workbench.bootstrap",
        name = "enabled",
        havingValue = "true"
    )
    public ApplicationRunner initialSupervisorSetup(
            ApplicationUserRepository users,
            PasswordEncoder passwordEncoder,
            Environment environment,
            PlatformTransactionManager transactionManager) {

        return arguments -> {
            String username = ApplicationUser.normalizeUsername(
                environment.getRequiredProperty(
                    "workbench.bootstrap.username"
                )
            );

            TransactionTemplate transaction =
                new TransactionTemplate(transactionManager);

            Boolean created = transaction.execute(transactionStatus -> {
                var existing = users.findByUsername(username);

                if (existing.isPresent()) {
                    ApplicationUser account = existing.get();

                    if (account.getRole() != ApplicationRole.SUPERVISOR
                            || !account.isActive()) {
                        throw new IllegalStateException(
                            "The setup username already belongs to "
                            + "an account that is not an active Supervisor."
                        );
                    }

                    // Existing Supervisor is preserved.
                    return false;
                }

                if (users.count() != 0) {
                    throw new IllegalStateException(
                        "Initial account setup is already complete. "
                        + "Disable bootstrap and use Supervisor "
                        + "user management for additional accounts."
                    );
                }

                String initialPassword =
                    environment.getRequiredProperty(
                        "workbench.bootstrap.password"
                    );

                validateInitialPassword(initialPassword);

                ApplicationUser supervisor = new ApplicationUser(
                    username,
                    passwordEncoder.encode(initialPassword),
                    ApplicationRole.SUPERVISOR,
                    "SETUP"
                );

                users.saveAndFlush(supervisor);
                return true;
            });

            if (Boolean.TRUE.equals(created)) {
                log.info("Initial Supervisor account created.");
            } else {
                log.info(
                    "Initial Supervisor already exists; "
                    + "no account or password was changed."
                );
            }
        };
    }

    private static void validateInitialPassword(String password) {
        boolean tooShort =
            password.codePointCount(0, password.length()) < 12;

        boolean tooLong =
            password.getBytes(StandardCharsets.UTF_8).length > 72;

        boolean containsControlCharacters =
            password.codePoints().anyMatch(Character::isISOControl);

        if (password.isBlank()
                || tooShort
                || tooLong
                || containsControlCharacters) {

            throw new IllegalArgumentException(
                "Use an initial password with at least 12 characters, "
                + "at most 72 UTF-8 bytes, and no control characters."
            );
        }
    }
}