package com.orbitto.auth.infrastructure.persistence;

import com.orbitto.auth.application.port.out.PasswordResetTokenStorePort;
import com.orbitto.auth.domain.identity.UserId;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

@Component
public class PasswordResetTokenStoreAdapter implements PasswordResetTokenStorePort {

    private final PasswordResetTokenJpaRepository jpa;

    public PasswordResetTokenStoreAdapter(PasswordResetTokenJpaRepository jpa) {
        this.jpa = jpa;
    }

    @Override
    @Transactional
    public void save(UserId userId, String tokenHash, Instant expiresAt, Instant createdAt) {
        PasswordResetTokenEntity e = new PasswordResetTokenEntity();
        e.setId(UUID.randomUUID());
        e.setUserId(userId.value());
        e.setTokenHash(tokenHash);
        e.setExpiresAt(expiresAt);
        e.setCreatedAt(createdAt);
        jpa.save(e);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<StoredResetToken> findByTokenHash(String tokenHash) {
        return jpa.findByTokenHash(tokenHash).map(this::toStored);
    }

    @Override
    @Transactional
    public void markUsed(UUID id, Instant usedAt) {
        jpa.markUsed(id, usedAt);
    }

    @Override
    @Transactional
    public void invalidateActiveForUser(UserId userId, Instant now) {
        jpa.invalidateActiveForUser(userId.value(), now);
    }

    private StoredResetToken toStored(PasswordResetTokenEntity e) {
        return new StoredResetToken(
                e.getId(),
                UserId.of(e.getUserId()),
                e.getTokenHash(),
                e.getExpiresAt(),
                e.getUsedAt()
        );
    }
}
