package com.orbitto.auth.application.command;

import com.orbitto.auth.application.port.out.ClockPort;
import com.orbitto.auth.application.port.out.PasswordHasherPort;
import com.orbitto.auth.application.port.out.RateLimiterPort;
import com.orbitto.auth.application.port.out.UserRepositoryPort;
import com.orbitto.auth.application.shared.ApplicationException;
import com.orbitto.auth.domain.identity.Email;
import com.orbitto.auth.domain.identity.PasswordHash;
import com.orbitto.auth.domain.identity.PlainPassword;
import com.orbitto.auth.domain.identity.User;
import com.orbitto.auth.domain.identity.UserId;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RegisterUserHandlerTest {

    @Mock
    UserRepositoryPort users;
    @Mock
    PasswordHasherPort passwordHasher;
    @Mock
    ClockPort clock;
    @Mock
    RateLimiterPort rateLimiter;

    RegisterUserHandler handler;

    @BeforeEach
    void setUp() {
        handler = new RegisterUserHandler(users, passwordHasher, clock, rateLimiter, new SimpleMeterRegistry());
    }

    @Test
    void savesUserWhenEmailFree() {
        when(clock.now()).thenReturn(Instant.parse("2026-01-01T00:00:00Z"));
        when(rateLimiter.tryConsume(RateLimiterPort.Bucket.REGISTER_IP, "k1")).thenReturn(true);
        when(users.existsByEmail(any(Email.class))).thenReturn(false);
        when(passwordHasher.hash(any(PlainPassword.class))).thenReturn(new PasswordHash("$2a$stub"));

        UserId id = handler.handle(new RegisterUserCommand("new@example.com", "password12345", "k1"));
        assertThat(id).isNotNull();
        ArgumentCaptor<User> cap = ArgumentCaptor.forClass(User.class);
        verify(users).save(cap.capture());
        assertThat(cap.getValue().email().value()).isEqualTo("new@example.com");
    }

    @Test
    void rejectsDuplicateEmail() {
        when(rateLimiter.tryConsume(RateLimiterPort.Bucket.REGISTER_IP, "k1")).thenReturn(true);
        when(users.existsByEmail(any(Email.class))).thenReturn(true);

        assertThatThrownBy(() -> handler.handle(new RegisterUserCommand("a@b.com", "password12345", "k1")))
                .isInstanceOf(ApplicationException.class)
                .hasFieldOrPropertyWithValue("code", "email_already_registered");
        verify(users, never()).save(any());
    }

    @Test
    void rejectsWhenRateLimited() {
        when(rateLimiter.tryConsume(RateLimiterPort.Bucket.REGISTER_IP, "k1")).thenReturn(false);

        assertThatThrownBy(() -> handler.handle(new RegisterUserCommand("a@b.com", "password12345", "k1")))
                .isInstanceOf(ApplicationException.class)
                .hasFieldOrPropertyWithValue("code", "rate_limited");
    }
}
