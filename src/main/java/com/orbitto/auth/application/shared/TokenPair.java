package com.orbitto.auth.application.shared;

import java.time.Instant;

public record TokenPair(
        String accessToken,
        String refreshToken,
        Instant accessExpiresAt,
        Instant refreshExpiresAt
) {}
