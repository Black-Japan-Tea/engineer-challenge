package com.orbitto.auth.infrastructure.security;

import com.orbitto.auth.application.port.out.AccessTokenIssuerPort;
import com.orbitto.auth.domain.identity.UserId;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;

@Component
public class JwtAccessTokenIssuer implements AccessTokenIssuerPort {

    private final SecretKey key;
    private final String issuer;
    private final int accessTtlMinutes;

    public JwtAccessTokenIssuer(
            @Value("${orbitto.auth.jwt.secret}") String secret,
            @Value("${orbitto.auth.jwt.issuer}") String issuer,
            @Value("${orbitto.auth.jwt.access-ttl-minutes:15}") int accessTtlMinutes) {
        this.key = Keys.hmacShaKeyFor(sha256(secret));
        this.issuer = issuer;
        this.accessTtlMinutes = accessTtlMinutes;
    }

    @Override
    public IssuedAccessToken issue(UserId userId, String email, Instant now) {
        Instant exp = now.plus(accessTtlMinutes, ChronoUnit.MINUTES);
        String jwt = Jwts.builder()
                .issuer(issuer)
                .subject(userId.value().toString())
                .claim("email", email)
                .issuedAt(Date.from(now))
                .expiration(Date.from(exp))
                .signWith(key)
                .compact();
        return new IssuedAccessToken(jwt, exp);
    }

    private static byte[] sha256(String secret) {
        try {
            return MessageDigest.getInstance("SHA-256").digest(secret.getBytes(StandardCharsets.UTF_8));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }
}
