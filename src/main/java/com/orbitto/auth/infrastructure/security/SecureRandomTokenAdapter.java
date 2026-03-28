package com.orbitto.auth.infrastructure.security;

import com.orbitto.auth.application.port.out.SecureRandomTokenPort;
import org.springframework.stereotype.Component;

import java.security.SecureRandom;
import java.util.Base64;

@Component
public class SecureRandomTokenAdapter implements SecureRandomTokenPort {

    private final SecureRandom random = new SecureRandom();

    @Override
    public String nextOpaqueToken(int numBytes) {
        byte[] buf = new byte[numBytes];
        random.nextBytes(buf);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(buf);
    }
}
