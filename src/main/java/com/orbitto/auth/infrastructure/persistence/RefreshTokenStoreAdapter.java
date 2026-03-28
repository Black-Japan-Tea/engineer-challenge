package com.orbitto.auth.infrastructure.persistence;

import com.orbitto.auth.application.port.out.RefreshTokenStorePort;
import com.orbitto.auth.domain.identity.UserId;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

@Component
public class RefreshTokenStoreAdapter implements RefreshTokenStorePort {

    private final RefreshTokenJpaRepository jpa;

    public RefreshTokenStoreAdapter(RefreshTokenJpaRepository jpa) {
        this.jpa = jpa;
    }

    @Override
    @Transactional
    public void persistNew(UserId userId, String tokenHash, UUID familyId, Instant expiresAt, Instant createdAt) {
        RefreshTokenEntity e = new RefreshTokenEntity();
        e.setId(UUID.randomUUID());
        e.setUserId(userId.value());
        e.setTokenHash(tokenHash);
        e.setFamilyId(familyId);
        e.setExpiresAt(expiresAt);
        e.setCreatedAt(createdAt);
        jpa.save(e);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<StoredRefreshToken> findByTokenHash(String tokenHash) {
        return jpa.findByTokenHash(tokenHash).map(this::toStored);
    }

    @Override
    @Transactional
    public void markRevoked(UUID id, Instant revokedAt, String replacedByHash) {
        jpa.markRevoked(id, revokedAt, replacedByHash);
    }

    @Override
    @Transactional
    public void revokeFamily(UUID familyId, Instant revokedAt) {
        jpa.revokeFamily(familyId, revokedAt);
    }

    private StoredRefreshToken toStored(RefreshTokenEntity e) {
        return new StoredRefreshToken(
                e.getId(),
                UserId.of(e.getUserId()),
                e.getTokenHash(),
                e.getFamilyId(),
                e.getExpiresAt(),
                e.getRevokedAt(),
                e.getReplacedByHash()
        );
    }
}
