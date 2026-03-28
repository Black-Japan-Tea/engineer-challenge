package com.orbitto.auth.application.command;

import com.orbitto.auth.application.port.out.ClockPort;
import com.orbitto.auth.application.port.out.OpaqueTokenHasherPort;
import com.orbitto.auth.application.port.out.PasswordHasherPort;
import com.orbitto.auth.application.port.out.PasswordResetTokenStorePort;
import com.orbitto.auth.application.port.out.UserRepositoryPort;
import com.orbitto.auth.application.shared.ApplicationException;
import com.orbitto.auth.domain.identity.PlainPassword;
import com.orbitto.auth.domain.shared.DomainException;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

@Service
public class ConfirmPasswordResetHandler {

    private final PasswordResetTokenStorePort resetStore;
    private final UserRepositoryPort users;
    private final PasswordHasherPort passwordHasher;
    private final OpaqueTokenHasherPort tokenHasher;
    private final ClockPort clock;
    private final Counter confirmed;
    private final Counter failed;

    public ConfirmPasswordResetHandler(PasswordResetTokenStorePort resetStore,
                                       UserRepositoryPort users,
                                       PasswordHasherPort passwordHasher,
                                       OpaqueTokenHasherPort tokenHasher,
                                       ClockPort clock,
                                       MeterRegistry meterRegistry) {
        this.resetStore = resetStore;
        this.users = users;
        this.passwordHasher = passwordHasher;
        this.tokenHasher = tokenHasher;
        this.clock = clock;
        this.confirmed = meterRegistry.counter("orbitto.auth.password_reset_confirmed", "result", "success");
        this.failed = meterRegistry.counter("orbitto.auth.password_reset_confirmed", "result", "failure");
    }

    @Transactional
    public void handle(ConfirmPasswordResetCommand cmd) {
        if (cmd.token() == null || cmd.token().isBlank()) {
            failed.increment();
            throw new ApplicationException("invalid_reset_token");
        }
        Instant now = clock.now();
        String hash = tokenHasher.hash(cmd.token());
        var rowOpt = resetStore.findByTokenHash(hash);
        if (rowOpt.isEmpty() || !rowOpt.get().consumable(now)) {
            failed.increment();
            throw new ApplicationException("invalid_reset_token");
        }
        var row = rowOpt.get();
        PlainPassword newPwd;
        try {
            newPwd = PlainPassword.of(cmd.newPassword());
        } catch (DomainException e) {
            failed.increment();
            throw new ApplicationException(e.code(), e);
        }
        var userOpt = users.findById(row.userId());
        if (userOpt.isEmpty()) {
            failed.increment();
            throw new ApplicationException("invalid_reset_token");
        }
        var user = userOpt.get();
        user.changePassword(passwordHasher.hash(newPwd), now);
        users.save(user);
        resetStore.markUsed(row.id(), now);
        confirmed.increment();
    }
}
