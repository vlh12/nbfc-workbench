package com.nbfc.shared.auth;

import java.io.Serial;
import java.io.Serializable;
import java.security.Principal;

public record WorkbenchPrincipal(
        String userId,
        String username,
        String role,
        long accountVersion) implements Principal, Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Override
    public String getName() {
        return userId;
    }
}