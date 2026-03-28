package com.orbitto.auth.application.command;

import com.orbitto.auth.application.port.out.AccessTokenIssuerPort;
import com.orbitto.auth.application.port.out.ClockPort;
import com.orbitto.auth.application.port.out.OpaqueTokenHasherPort;
import com.orbitto.auth.application.port.out.RefreshTokenStorePort;
import com.orbitto.auth.application.port.out.SecureRandomTokenPort;
import com.orbitto.auth.application.port.out.UserRepositoryPort;
import com.orbitto.auth.application.shared.ApplicationException;
import com.orbitto.auth.domain.identity.UserId;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RefreshTokenHandlerReplayTest {

    static final Instant NOW = Instant.parse("2026-03-01T12:00:00Z");
    static final UserId UID = UserId.of(UUID.randomUUID());
    static final UUID ROW_ID = UUID.randomUUID();
    static final UUID FAMILY = UUID.randomUUID();

    @Mock
    RefreshTokenStorePort refreshTokens;
    @Mock
    UserRepositoryPort users;
    @Mock
    OpaqueTokenHasherPort tokenHasher;
    @Mock
    SecureRandomTokenPort randomToken;
    @Mock
    AccessTokenIssuerPort accessTokens;
    @Mock
    ClockPort clock;

    @Test
    void reuseOfRotatedTokenRevokesFamily() {
        RefreshTokenHandler handler = new RefreshTokenHandler(
                refreshTokens, users, tokenHasher, randomToken, accessTokens,
                clock,
                7,
                new SimpleMeterRegistry()
        );
        when(clock.now()).thenReturn(NOW);
        when(tokenHasher.hash("old")).thenReturn("h1");
        when(refreshTokens.findByTokenHash("h1")).thenReturn(Optional.of(
                new RefreshTokenStorePort.StoredRefreshToken(
                        ROW_ID, UID, "h1", FAMILY, NOW.plusSeconds(3600), NOW.minusSeconds(1), "h2")));

        assertThatThrownBy(() -> handler.handle(new RefreshTokenCommand("old")))
                .isInstanceOf(ApplicationException.class)
                .hasFieldOrPropertyWithValue("code", "refresh_token_reuse");
        verify(refreshTokens).revokeFamily(eq(FAMILY), eq(NOW));
    }
}
