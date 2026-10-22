package com.nbfc.accessdata.auth;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ApplicationUserRepository
        extends JpaRepository<ApplicationUser, String> {

    Optional<ApplicationUser> findByUsername(String username);
}