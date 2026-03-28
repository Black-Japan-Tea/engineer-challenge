package com.orbitto.auth.application.port.out;

import com.orbitto.auth.domain.identity.UserId;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

public interface PasswordResetTokenStorePort {

    record StoredResetToken(
            UUID id,
            UserId userId,
            String tokenHash,
            Instant expiresAt,
            Instant usedAt
    ) {
        public boolean consumable(Instant now) {
            return usedAt == null && now.isBefore(expiresAt);
        }
    }

    void save(UserId userId, String tokenHash, Instant expiresAt, Instant createdAt);

    Optional<StoredResetToken> findByTokenHash(String tokenHash);

    void markUsed(UUID id, Instant usedAt);

    /** Сбрасывает прочие неиспользованные токены пользователя — один актуальный reset за раз. */
    void invalidateActiveForUser(UserId userId, Instant now);
}
