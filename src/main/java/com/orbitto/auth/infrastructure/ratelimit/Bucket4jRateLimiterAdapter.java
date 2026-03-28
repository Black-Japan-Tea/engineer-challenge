package com.orbitto.auth.infrastructure.ratelimit;

import com.orbitto.auth.application.port.out.RateLimiterPort;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.LoadingCache;
import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Refill;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.Duration;

@Component
public class Bucket4jRateLimiterAdapter implements RateLimiterPort {

    private final LoadingCache<String, io.github.bucket4j.Bucket> loginByEmail;
    private final LoadingCache<String, io.github.bucket4j.Bucket> registerByClient;
    private final LoadingCache<String, io.github.bucket4j.Bucket> resetByEmail;

    public Bucket4jRateLimiterAdapter(
            @Value("${orbitto.auth.rate-limit.login-per-email-per-window:5}") int loginPerWindow,
            @Value("${orbitto.auth.rate-limit.login-window-minutes:15}") int loginWindowMinutes,
            @Value("${orbitto.auth.rate-limit.register-per-ip-per-hour:20}") int registerPerHour,
            @Value("${orbitto.auth.rate-limit.password-reset-per-email-per-hour:3}") int resetPerHour) {
        Duration loginWindow = Duration.ofMinutes(loginWindowMinutes);
        this.loginByEmail = Caffeine.newBuilder()
                .maximumSize(50_000)
                .build(k -> io.github.bucket4j.Bucket.builder()
                        .addLimit(Bandwidth.classic(loginPerWindow, Refill.intervally(loginPerWindow, loginWindow)))
                        .build());
        this.registerByClient = Caffeine.newBuilder()
                .maximumSize(50_000)
                .build(k -> io.github.bucket4j.Bucket.builder()
                        .addLimit(Bandwidth.classic(registerPerHour, Refill.intervally(registerPerHour, Duration.ofHours(1))))
                        .build());
        this.resetByEmail = Caffeine.newBuilder()
                .maximumSize(50_000)
                .build(k -> io.github.bucket4j.Bucket.builder()
                        .addLimit(Bandwidth.classic(resetPerHour, Refill.intervally(resetPerHour, Duration.ofHours(1))))
                        .build());
    }

    @Override
    public boolean tryConsume(Bucket bucket, String key) {
        LoadingCache<String, io.github.bucket4j.Bucket> cache = switch (bucket) {
            case LOGIN_EMAIL -> loginByEmail;
            case REGISTER_IP -> registerByClient;
            case PASSWORD_RESET_EMAIL -> resetByEmail;
        };
        return cache.get(key).tryConsume(1);
    }
}
