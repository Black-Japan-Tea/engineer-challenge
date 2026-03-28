package com.orbitto.auth.domain.identity;

import java.util.UUID;

public record UserId(UUID value) {

    public static UserId of(UUID uuid) {
        return new UserId(uuid);
    }

    public static UserId generate() {
        return new UserId(UUID.randomUUID());
    }
}
