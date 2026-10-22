package com.nbfc.accessdata.auth;

import java.util.Optional;
import java.util.stream.Stream;

import com.nbfc.accessdata.auth.LoginChallengeService.VerifiedLogin;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;

import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.server.ResponseStatusException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

class PasswordChangeServiceTest {

    private static final String CURRENT_PASSWORD =
            "SyntheticCurrentPassword2026!";

    private static final String NEW_PASSWORD =
            "SyntheticReplacementPassword2026!";

    private ApplicationUserRepository users;
    private JdbcTemplate jdbc;
    private PasswordEncoder passwords;
    private PasswordChangeService service;

    private ApplicationUser user;
    private VerifiedLogin proof;

    @BeforeEach
    void setUp() {
        users = mock(ApplicationUserRepository.class);
        jdbc = mock(JdbcTemplate.class);

        // Lower cost keeps unit tests fast.
        // This does not change the application's password encoder.
        passwords = new BCryptPasswordEncoder(4);

        service = new PasswordChangeService(
                users,
                passwords,
                jdbc
        );

        user = new ApplicationUser(
                "test-user",
                passwords.encode(CURRENT_PASSWORD),
                ApplicationRole.ANALYST,
                "SETUP"
        );

        ReflectionTestUtils.setField(user, "version", 7L);

        proof = new VerifiedLogin(
                user.getId(),
                user.getUsername(),
                user.getRole().name(),
                7L
        );
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "CONTRIBUTOR",
            "ANALYST",
            "SUPERVISOR"
    })
    void changesPasswordAndTargetsOnlyThatAccount(String role) {
        ReflectionTestUtils.setField(
                user,
                "role",
                ApplicationRole.valueOf(role)
        );

        var verified = new VerifiedLogin(
                user.getId(),
                user.getUsername(),
                role,
                7L
        );

        loadUser();

        String previousHash = user.getPasswordHash();

        service.changePassword(
                verified,
                futureExpiry(),
                NEW_PASSWORD,
                NEW_PASSWORD
        );

        assertThat(user.getPasswordHash())
                .isNotEqualTo(previousHash)
                .isNotEqualTo(NEW_PASSWORD);

        assertThat(passwords.matches(
                NEW_PASSWORD,
                user.getPasswordHash()
        )).isTrue();

        assertThat(passwords.matches(
                CURRENT_PASSWORD,
                user.getPasswordHash()
        )).isFalse();

        assertThat(user.getPasswordChangedAt()).isNotNull();

        verify(users).saveAndFlush(user);

        verify(jdbc).update(
                "DELETE FROM wb_session WHERE principal_name = ?",
                user.getId()
        );

        verify(jdbc).update(
                "DELETE FROM wb_login_challenge WHERE user_id = ?",
                user.getId()
        );

        verifyNoMoreInteractions(jdbc);
    }

    @ParameterizedTest
    @MethodSource("invalidPasswords")
    void rejectsInvalidPasswordBeforeDatabaseAccess(
            String newPassword,
            String confirmation) {

        expectStatus(400, () -> service.changePassword(
                proof,
                futureExpiry(),
                newPassword,
                confirmation
        ));

        verifyNoInteractions(users, jdbc);
    }

    static Stream<Arguments> invalidPasswords() {
        return Stream.of(
                Arguments.of((String) null, (String) null),
                Arguments.of("", ""),
                Arguments.of(" ".repeat(12), " ".repeat(12)),
                Arguments.of("a".repeat(11), "a".repeat(11)),
                Arguments.of(
                        "\uD83D\uDE00".repeat(11),
                        "\uD83D\uDE00".repeat(11)
                ),
                Arguments.of("a".repeat(73), "a".repeat(73)),
                Arguments.of(
                        "\u00e9".repeat(37),
                        "\u00e9".repeat(37)
                ),
                Arguments.of(
                        NEW_PASSWORD,
                        "DifferentReplacementPassword2026!"
                )
        );
    }

    @ParameterizedTest
    @MethodSource("acceptedBoundaryPasswords")
    void acceptsValidPasswordLengthBoundaries(String newPassword) {
        loadUser();

        service.changePassword(
                proof,
                futureExpiry(),
                newPassword,
                newPassword
        );

        assertThat(passwords.matches(
                newPassword,
                user.getPasswordHash()
        )).isTrue();

        verify(users).saveAndFlush(user);
    }

    static Stream<Arguments> acceptedBoundaryPasswords() {
        return Stream.of(
                Arguments.of("abcdefghijkl"),
                Arguments.of("a".repeat(72)),
                Arguments.of("\uD83D\uDE00".repeat(18))
        );
    }

    @Test
    void rejectsExpiredVerificationBeforeDatabaseAccess() {
        expectStatus(401, () -> service.changePassword(
                proof,
                System.currentTimeMillis() - 1_000,
                NEW_PASSWORD,
                NEW_PASSWORD
        ));

        verifyNoInteractions(users, jdbc);
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "missing",
            "inactive",
            "username",
            "role",
            "version",
            "unversioned"
    })
    void rejectsMissingOrChangedAccount(String change) {
        switch (change) {
            case "missing" -> {
                // The repository returns no account below.
            }
            case "inactive" ->
                    ReflectionTestUtils.setField(
                            user, "status", "DISABLED"
                    );
            case "username" ->
                    ReflectionTestUtils.setField(
                            user, "username", "changed-user"
                    );
            case "role" ->
                    ReflectionTestUtils.setField(
                            user, "role", ApplicationRole.SUPERVISOR
                    );
            case "version" ->
                    ReflectionTestUtils.setField(
                            user, "version", 8L
                    );
            case "unversioned" ->
                    ReflectionTestUtils.setField(
                            user, "version", null
                    );
            default -> throw new IllegalArgumentException(change);
        }

        when(users.findById(proof.userId())).thenReturn(
                "missing".equals(change)
                        ? Optional.empty()
                        : Optional.of(user)
        );

        expectStatus(401, () -> service.changePassword(
                proof,
                futureExpiry(),
                NEW_PASSWORD,
                NEW_PASSWORD
        ));

        verify(users, never())
                .saveAndFlush(any(ApplicationUser.class));

        verifyNoInteractions(jdbc);
    }

    @Test
    void rejectsUsingTheCurrentPasswordAgain() {
        loadUser();

        String previousHash = user.getPasswordHash();

        expectStatus(400, () -> service.changePassword(
                proof,
                futureExpiry(),
                CURRENT_PASSWORD,
                CURRENT_PASSWORD
        ));

        assertThat(user.getPasswordHash()).isEqualTo(previousHash);

        verify(users, never())
                .saveAndFlush(any(ApplicationUser.class));

        verifyNoInteractions(jdbc);
    }

    @Test
    void propagatesConcurrentAccountUpdateFailure() {
        loadUser();

        var failure = new OptimisticLockingFailureException(
                "Synthetic concurrent account update"
        );

        when(users.saveAndFlush(user)).thenThrow(failure);

        assertThatThrownBy(() -> service.changePassword(
                proof,
                futureExpiry(),
                NEW_PASSWORD,
                NEW_PASSWORD
        )).isSameAs(failure);

        verifyNoInteractions(jdbc);
    }

    @Test
    void propagatesSessionCleanupFailure() {
        loadUser();

        var failure = new DataAccessResourceFailureException(
                "Synthetic session cleanup failure"
        );

        when(jdbc.update(
                "DELETE FROM wb_session WHERE principal_name = ?",
                user.getId()
        )).thenThrow(failure);

        assertThatThrownBy(() -> service.changePassword(
                proof,
                futureExpiry(),
                NEW_PASSWORD,
                NEW_PASSWORD
        )).isSameAs(failure);

        verify(users).saveAndFlush(user);

        verify(jdbc, never()).update(
                "DELETE FROM wb_login_challenge WHERE user_id = ?",
                user.getId()
        );
    }

    private void loadUser() {
        when(users.findById(user.getId()))
                .thenReturn(Optional.of(user));
    }

    private static long futureExpiry() {
        return System.currentTimeMillis() + 300_000;
    }

    private static void expectStatus(
            int expectedStatus,
            Runnable action) {

        assertThatThrownBy(action::run)
                .isInstanceOfSatisfying(
                        ResponseStatusException.class,
                        exception -> assertThat(
                                exception.getStatusCode().value()
                        ).isEqualTo(expectedStatus)
                );
    }
}