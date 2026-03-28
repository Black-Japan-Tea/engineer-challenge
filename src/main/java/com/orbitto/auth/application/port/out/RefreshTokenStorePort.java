package com.orbitto.auth.application.port.out;

import com.orbitto.auth.domain.identity.UserId;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

public interface RefreshTokenStorePort {

    record StoredRefreshToken(
            UUID id,
            UserId userId,
            String tokenHash,
            UUID familyId,
            Instant expiresAt,
            Instant revokedAt,
            String replacedByHash
    ) {
        public boolean isActive(Instant now) {
            return revokedAt == null && expiresAt != null && now.isBefore(expiresAt);
        }
    }

    void persistNew(UserId userId, String tokenHash, UUID familyId, Instant expiresAt, Instant createdAt);

    Optional<StoredRefreshToken> findByTokenHash(String tokenHash);

    void markRevoked(UUID id, Instant revokedAt, String replacedByHash);

    void revokeFamily(UUID familyId, Instant revokedAt);
}
