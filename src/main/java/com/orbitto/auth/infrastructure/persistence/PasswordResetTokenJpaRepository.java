package com.orbitto.auth.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

public interface PasswordResetTokenJpaRepository extends JpaRepository<PasswordResetTokenEntity, UUID> {

    Optional<PasswordResetTokenEntity> findByTokenHash(String tokenHash);

    @Modifying
    @Query("UPDATE PasswordResetTokenEntity t SET t.usedAt = :usedAt WHERE t.id = :id")
    int markUsed(@Param("id") UUID id, @Param("usedAt") Instant usedAt);

    @Modifying
    @Query("UPDATE PasswordResetTokenEntity t SET t.usedAt = :now WHERE t.userId = :userId AND t.usedAt IS NULL")
    int invalidateActiveForUser(@Param("userId") UUID userId, @Param("now") Instant now);
}
