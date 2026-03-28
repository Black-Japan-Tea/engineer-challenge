package com.orbitto.auth.application.port.out;

import com.orbitto.auth.domain.identity.UserId;

import java.time.Instant;

public interface AccessTokenIssuerPort {

    IssuedAccessToken issue(UserId userId, String email, Instant now);

    record IssuedAccessToken(String jwt, Instant expiresAt) {}
}
