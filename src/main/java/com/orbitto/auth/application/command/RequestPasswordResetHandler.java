package com.orbitto.auth.application.command;

import com.orbitto.auth.application.port.out.ClockPort;
import com.orbitto.auth.application.port.out.PasswordResetNotificationPort;
import com.orbitto.auth.application.port.out.PasswordResetTokenStorePort;
import com.orbitto.auth.application.port.out.RateLimiterPort;
import com.orbitto.auth.application.port.out.SecureRandomTokenPort;
import com.orbitto.auth.application.port.out.OpaqueTokenHasherPort;
import com.orbitto.auth.application.port.out.UserRepositoryPort;
import com.orbitto.auth.domain.identity.Email;
import com.orbitto.auth.domain.shared.DomainException;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;

@Service
public class RequestPasswordResetHandler {

    private final UserRepositoryPort users;
    private final PasswordResetTokenStorePort resetStore;
    private final SecureRandomTokenPort randomToken;
    private final OpaqueTokenHasherPort tokenHasher;
    private final PasswordResetNotificationPort notification;
    private final ClockPort clock;
    private final RateLimiterPort rateLimiter;
    private final Duration tokenTtl;
    private final Counter resetRequested;

    public RequestPasswordResetHandler(UserRepositoryPort users,
                                       PasswordResetTokenStorePort resetStore,
                                       SecureRandomTokenPort randomToken,
                                       OpaqueTokenHasherPort tokenHasher,
                                       PasswordResetNotificationPort notification,
                                       ClockPort clock,
                                       RateLimiterPort rateLimiter,
                                       @Value("${orbitto.auth.password-reset.token-ttl-minutes:60}") int ttlMinutes,
                                       MeterRegistry meterRegistry) {
        this.users = users;
        this.resetStore = resetStore;
        this.randomToken = randomToken;
        this.tokenHasher = tokenHasher;
        this.notification = notification;
        this.clock = clock;
        this.rateLimiter = rateLimiter;
        this.tokenTtl = Duration.ofMinutes(ttlMinutes);
        this.resetRequested = meterRegistry.counter("orbitto.auth.password_reset_requested");
    }

    /** Всегда отрабатывает «тихо»: не палим, есть ли аккаунт; письмо/токен — только если пользователь есть и лимит не выбит. */
    @Transactional
    public void handle(RequestPasswordResetCommand cmd) {
        Email email;
        try {
            email = Email.of(cmd.email());
        } catch (DomainException e) {
            return;
        }
        if (!rateLimiter.tryConsume(RateLimiterPort.Bucket.PASSWORD_RESET_EMAIL, email.value())) {
            return;
        }
        var userOpt = users.findByEmail(email);
        if (userOpt.isEmpty()) {
            return;
        }
        var user = userOpt.get();
        Instant now = clock.now();
        resetStore.invalidateActiveForUser(user.id(), now);
        String raw = randomToken.nextOpaqueToken(32);
        String hash = tokenHasher.hash(raw);
        Instant exp = now.plus(tokenTtl);
        resetStore.save(user.id(), hash, exp, now);
        notification.sendResetLink(email, raw);
        resetRequested.increment();
    }
}
