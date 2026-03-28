package com.orbitto.auth.application.port.out;

public interface RateLimiterPort {

    enum Bucket {
        LOGIN_EMAIL,
        REGISTER_IP,
        PASSWORD_RESET_EMAIL
    }

    boolean tryConsume(Bucket bucket, String key);
}
