package com.nbfc.accessdata.auth;

import com.nbfc.accessdata.auth.LoginChallengeService.VerifiedLogin;

import java.nio.charset.StandardCharsets;

import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class PasswordChangeService {

    private final ApplicationUserRepository users;
    private final PasswordEncoder passwords;
    private final JdbcTemplate jdbc;

    public PasswordChangeService(
            ApplicationUserRepository users,
            PasswordEncoder passwords,
            JdbcTemplate jdbc) {

        this.users = users;
        this.passwords = passwords;
        this.jdbc = jdbc;
    }

    @Transactional
    public void changePassword(
            VerifiedLogin verified,
            long expiresAtEpochMillis,
            String newPassword,
            String confirmNewPassword) {

        requireFreshVerification(
                verified,
                expiresAtEpochMillis
        );

        validateNewPassword(newPassword, confirmNewPassword);

        ApplicationUser account = users
                .findById(verified.userId())
                .orElseThrow(
                        PasswordChangeService::invalidVerification
                );

        if (!account.isActive()
                || !account.getUsername().equals(verified.username())
                || !account.getRole().name().equals(verified.role())
                || account.getVersion() == null
                || account.getVersion().longValue()
                        != verified.accountVersion()) {

            throw invalidVerification();
        }

        if (passwords.matches(
                newPassword,
                account.getPasswordHash())) {

            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Choose a password different from your current password."
            );
        }

        String encodedPassword = passwords.encode(newPassword);

        account.changePasswordHash(encodedPassword);

        // Flush also increments the account's existing @Version field.
        users.saveAndFlush(account);

        // A database wait must not allow expired verification to succeed.
        requireFreshVerification(
                verified,
                expiresAtEpochMillis
        );

        // Existing session attributes are removed by the cascading FK.
        jdbc.update(
                "DELETE FROM wb_session WHERE principal_name = ?",
                verified.userId()
        );

        // Remove pending or previously used login codes for this account.
        jdbc.update(
                "DELETE FROM wb_login_challenge WHERE user_id = ?",
                verified.userId()
        );
    }

    public static void validateNewPassword(
            String newPassword,
            String confirmNewPassword) {

        if (newPassword == null
                || newPassword.isBlank()
                || newPassword.codePointCount(
                        0,
                        newPassword.length()
                ) < 12) {

            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "New password must contain at least 12 characters."
            );
        }

        if (newPassword.length() > 72
                || newPassword.getBytes(
                        StandardCharsets.UTF_8
                ).length > 72) {

            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "New password is too long. "
                    + "Use no more than 72 UTF-8 bytes."
            );
        }

        if (!newPassword.equals(confirmNewPassword)) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "The new passwords do not match."
            );
        }
    }

    private static void requireFreshVerification(
            VerifiedLogin verified,
            long expiresAtEpochMillis) {

        if (verified == null
                || verified.userId() == null
                || expiresAtEpochMillis <= System.currentTimeMillis()) {

            throw invalidVerification();
        }
    }

    private static ResponseStatusException invalidVerification() {
        return new ResponseStatusException(
                HttpStatus.UNAUTHORIZED,
                "Your password-change verification expired or the account "
                + "changed. Verify your current credentials again."
        );
    }
}