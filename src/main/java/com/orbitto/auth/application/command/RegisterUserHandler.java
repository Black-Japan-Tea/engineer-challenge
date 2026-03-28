package com.orbitto.auth.application.command;

import com.orbitto.auth.application.port.out.ClockPort;
import com.orbitto.auth.application.port.out.PasswordHasherPort;
import com.orbitto.auth.application.port.out.RateLimiterPort;
import com.orbitto.auth.application.port.out.UserRepositoryPort;
import com.orbitto.auth.application.shared.ApplicationException;
import com.orbitto.auth.domain.identity.Email;
import com.orbitto.auth.domain.identity.PlainPassword;
import com.orbitto.auth.domain.identity.User;
import com.orbitto.auth.domain.identity.UserId;
import com.orbitto.auth.domain.shared.DomainException;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class RegisterUserHandler {

    private final UserRepositoryPort users;
    private final PasswordHasherPort passwordHasher;
    private final ClockPort clock;
    private final RateLimiterPort rateLimiter;
    private final Counter registrations;

    public RegisterUserHandler(UserRepositoryPort users,
                               PasswordHasherPort passwordHasher,
                               ClockPort clock,
                               RateLimiterPort rateLimiter,
                               MeterRegistry meterRegistry) {
        this.users = users;
        this.passwordHasher = passwordHasher;
        this.clock = clock;
        this.rateLimiter = rateLimiter;
        this.registrations = meterRegistry.counter("orbitto.auth.registrations");
    }

    @Transactional
    public UserId handle(RegisterUserCommand cmd) {
        String rateKey = cmd.clientKeyForRateLimit() != null ? cmd.clientKeyForRateLimit() : "anonymous";
        if (!rateLimiter.tryConsume(RateLimiterPort.Bucket.REGISTER_IP, rateKey)) {
            throw new ApplicationException("rate_limited");
        }
        try {
            Email email = Email.of(cmd.email());
            PlainPassword pwd = PlainPassword.of(cmd.password());
            if (users.existsByEmail(email)) {
                throw new ApplicationException("email_already_registered");
            }
            var hash = passwordHasher.hash(pwd);
            User user = User.register(UserId.generate(), email, hash, clock.now());
            users.save(user);
            registrations.increment();
            return user.id();
        } catch (DomainException e) {
            throw new ApplicationException(e.code(), e);
        }
    }
}
