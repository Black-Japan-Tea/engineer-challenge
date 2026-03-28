package com.orbitto.auth.application.command;

import com.orbitto.auth.application.port.out.AccessTokenIssuerPort;
import com.orbitto.auth.application.port.out.ClockPort;
import com.orbitto.auth.application.port.out.OpaqueTokenHasherPort;
import com.orbitto.auth.application.port.out.PasswordHasherPort;
import com.orbitto.auth.application.port.out.RateLimiterPort;
import com.orbitto.auth.application.port.out.RefreshTokenStorePort;
import com.orbitto.auth.application.port.out.SecureRandomTokenPort;
import com.orbitto.auth.application.port.out.UserRepositoryPort;
import com.orbitto.auth.application.shared.ApplicationException;
import com.orbitto.auth.application.shared.TokenPair;
import com.orbitto.auth.domain.identity.Email;
import com.orbitto.auth.domain.shared.DomainException;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

@Service
public class LoginHandler {

    private final UserRepositoryPort users;
    private final PasswordHasherPort passwordHasher;
    private final AccessTokenIssuerPort accessTokens;
    private final RefreshTokenStorePort refreshTokens;
    private final OpaqueTokenHasherPort tokenHasher;
    private final SecureRandomTokenPort randomToken;
    private final ClockPort clock;
    private final RateLimiterPort rateLimiter;
    private final Duration refreshTtl;
    private final Counter loginSuccess;
    private final Counter loginFailure;

    public LoginHandler(UserRepositoryPort users,
                        PasswordHasherPort passwordHasher,
                        AccessTokenIssuerPort accessTokens,
                        RefreshTokenStorePort refreshTokens,
                        OpaqueTokenHasherPort tokenHasher,
                        SecureRandomTokenPort randomToken,
                        ClockPort clock,
                        RateLimiterPort rateLimiter,
                        @Value("${orbitto.auth.jwt.refresh-ttl-days:7}") int refreshTtlDays,
                        MeterRegistry meterRegistry) {
        this.users = users;
        this.passwordHasher = passwordHasher;
        this.accessTokens = accessTokens;
        this.refreshTokens = refreshTokens;
        this.tokenHasher = tokenHasher;
        this.randomToken = randomToken;
        this.clock = clock;
        this.rateLimiter = rateLimiter;
        this.refreshTtl = Duration.ofDays(refreshTtlDays);
        this.loginSuccess = meterRegistry.counter("orbitto.auth.login", "result", "success");
        this.loginFailure = meterRegistry.counter("orbitto.auth.login", "result", "failure");
    }

    @Transactional
    public TokenPair handle(LoginCommand cmd) {
        Email email;
        try {
            email = Email.of(cmd.email());
        } catch (DomainException e) {
            loginFailure.increment();
            throw new ApplicationException("invalid_credentials");
        }
        if (!rateLimiter.tryConsume(RateLimiterPort.Bucket.LOGIN_EMAIL, email.value())) {
            loginFailure.increment();
            throw new ApplicationException("rate_limited");
        }
        var userOpt = users.findByEmail(email);
        if (userOpt.isEmpty()) {
            loginFailure.increment();
            throw new ApplicationException("invalid_credentials");
        }
        var user = userOpt.get();
        if (cmd.password() == null || !passwordHasher.matchesRaw(cmd.password(), user.passwordHash())) {
            loginFailure.increment();
            throw new ApplicationException("invalid_credentials");
        }
        Instant now = clock.now();
        var access = accessTokens.issue(user.id(), user.email().value(), now);
        String rawRefresh = randomToken.nextOpaqueToken(32);
        String refreshHash = tokenHasher.hash(rawRefresh);
        UUID familyId = UUID.randomUUID();
        Instant refreshExp = now.plus(refreshTtl);
        refreshTokens.persistNew(user.id(), refreshHash, familyId, refreshExp, now);
        loginSuccess.increment();
        return new TokenPair(access.jwt(), rawRefresh, access.expiresAt(), refreshExp);
    }
}
