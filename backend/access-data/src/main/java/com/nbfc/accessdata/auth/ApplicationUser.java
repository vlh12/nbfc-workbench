package com.nbfc.accessdata.auth;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

import java.time.Instant;
import java.util.Locale;
import java.util.Objects;
import java.util.UUID;

@Entity
@Table(name = "wb_app_user")
public class ApplicationUser {

    @Id
    @Column(name = "id", nullable = false, length = 36)
    private String id;

    @Column(name = "username", nullable = false, length = 64)
    private String username;

    @JsonIgnore
    @Column(name = "password_hash", nullable = false, length = 100)
    private String passwordHash;

    @Enumerated(EnumType.STRING)
    @Column(name = "role_name", nullable = false, length = 16)
    private ApplicationRole role;

    @Column(name = "status", nullable = false, length = 16)
    private String status;

    @Column(name = "created_by", nullable = false, length = 36)
    private String createdBy;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "password_changed_at")
    private Instant passwordChangedAt;

    @Version
    @Column(name = "version", nullable = false)
    private Long version;

    protected ApplicationUser() {
        // Required by JPA.
    }

    public ApplicationUser(
            String username,
            String passwordHash,
            ApplicationRole role,
            String createdBy) {

        this.id = UUID.randomUUID().toString();
        this.username = normalizeUsername(username);
        this.passwordHash = Objects.requireNonNull(passwordHash);
        this.role = Objects.requireNonNull(role);
        this.status = "ACTIVE";
        this.createdBy = Objects.requireNonNull(createdBy);
        this.createdAt = Instant.now();
    }

    public static String normalizeUsername(String value) {
        if (value == null) {
            throw new IllegalArgumentException("Username is required.");
        }

        String normalized = value.strip().toLowerCase(Locale.ROOT);

        if (!normalized.matches("[a-z][a-z0-9._-]{2,63}")) {
            throw new IllegalArgumentException(
                "Username must contain 3 to 64 characters, start with "
                + "a letter, and use letters, digits, dots, underscores "
                + "or hyphens."
            );
        }

        return normalized;
    }

    public String getId() {
        return id;
    }

    public String getUsername() {
        return username;
    }

    @JsonIgnore
    public String getPasswordHash() {
        return passwordHash;
    }

    public ApplicationRole getRole() {
        return role;
    }

    public String getStatus() {
        return status;
    }

    public boolean isActive() {
        return "ACTIVE".equals(status);
    }

    public String getCreatedBy() {
        return createdBy;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getPasswordChangedAt() {
        return passwordChangedAt;
    }

    public Long getVersion() {
        return version;
    }

    public void changePasswordHash(String encodedPassword) {
        this.passwordHash = Objects.requireNonNull(encodedPassword);
        this.passwordChangedAt = Instant.now();
    }
}