package com.orbitto.auth.domain.identity;

public record PasswordHash(String value) {

    public PasswordHash {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("passwordHash required");
        }
    }
}
