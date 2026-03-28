package com.orbitto.auth.application.command;

import com.orbitto.auth.application.port.out.AccessTokenIssuerPort;
import com.orbitto.auth.application.port.out.ClockPort;
import com.orbitto.auth.application.port.out.OpaqueTokenHasherPort;
import com.orbitto.auth.application.port.out.RefreshTokenStorePort;
import com.orbitto.auth.application.port.out.SecureRandomTokenPort;
import com.orbitto.auth.application.port.out.UserRepositoryPort;
import com.orbitto.auth.application.shared.ApplicationException;
import com.orbitto.auth.application.shared.TokenPair;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

@Service
public class RefreshTokenHandler {

    private final RefreshTokenStorePort refreshTokens;
    private final UserRepositoryPort users;
    private final OpaqueTokenHasherPort tokenHasher;
    private final SecureRandomTokenPort randomToken;
    private final AccessTokenIssuerPort accessTokens;
    private final ClockPort clock;
    private final Duration refreshTtl;
    private final Counter rotationSuccess;
    private final Counter rotationFailure;
    private final Counter reuseDetected;

    public RefreshTokenHandler(RefreshTokenStorePort refreshTokens,
                               UserRepositoryPort users,
                               OpaqueTokenHasherPort tokenHasher,
                               SecureRandomTokenPort randomToken,
                               AccessTokenIssuerPort accessTokens,
                               ClockPort clock,
                               @Value("${orbitto.auth.jwt.refresh-ttl-days:7}") int refreshTtlDays,
                               MeterRegistry meterRegistry) {
        this.refreshTokens = refreshTokens;
        this.users = users;
        this.tokenHasher = tokenHasher;
        this.randomToken = randomToken;
        this.accessTokens = accessTokens;
        this.clock = clock;
        this.refreshTtl = Duration.ofDays(refreshTtlDays);
        this.rotationSuccess = meterRegistry.counter("orbitto.auth.refresh", "result", "success");
        this.rotationFailure = meterRegistry.counter("orbitto.auth.refresh", "result", "failure");
        this.reuseDetected = meterRegistry.counter("orbitto.auth.refresh_reuse_detected");
    }

    @Transactional
    public TokenPair handle(RefreshTokenCommand cmd) {
        if (cmd.refreshToken() == null || cmd.refreshToken().isBlank()) {
            rotationFailure.increment();
            throw new ApplicationException("invalid_refresh_token");
        }
        String hash = tokenHasher.hash(cmd.refreshToken());
        Instant now = clock.now();
        var rowOpt = refreshTokens.findByTokenHash(hash);
        if (rowOpt.isEmpty()) {
            rotationFailure.increment();
            throw new ApplicationException("invalid_refresh_token");
        }
        var row = rowOpt.get();
        if (row.revokedAt() != null && row.replacedByHash() != null) {
            reuseDetected.increment();
            refreshTokens.revokeFamily(row.familyId(), now);
            rotationFailure.increment();
            throw new ApplicationException("refresh_token_reuse");
        }
        if (!row.isActive(now)) {
            rotationFailure.increment();
            throw new ApplicationException("invalid_refresh_token");
        }
        var userOpt = users.findById(row.userId());
        if (userOpt.isEmpty()) {
            rotationFailure.increment();
            throw new ApplicationException("invalid_refresh_token");
        }
        var user = userOpt.get();
        String newRaw = randomToken.nextOpaqueToken(32);
        String newHash = tokenHasher.hash(newRaw);
        Instant refreshExp = now.plus(refreshTtl);
        refreshTokens.persistNew(user.id(), newHash, row.familyId(), refreshExp, now);
        refreshTokens.markRevoked(row.id(), now, newHash);
        var access = accessTokens.issue(user.id(), user.email().value(), now);
        rotationSuccess.increment();
        return new TokenPair(access.jwt(), newRaw, access.expiresAt(), refreshExp);
    }
}
