package com.orbitto.auth.infrastructure.security;

import com.orbitto.auth.application.port.out.PasswordHasherPort;
import com.orbitto.auth.domain.identity.PasswordHash;
import com.orbitto.auth.domain.identity.PlainPassword;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Component;

@Component
public class BcryptPasswordHasher implements PasswordHasherPort {

    private final BCryptPasswordEncoder encoder = new BCryptPasswordEncoder(12);

    @Override
    public PasswordHash hash(PlainPassword plain) {
        return new PasswordHash(encoder.encode(plain.value()));
    }

    @Override
    public boolean matches(PlainPassword plain, PasswordHash hash) {
        return encoder.matches(plain.value(), hash.value());
    }

    @Override
    public boolean matchesRaw(String plainText, PasswordHash hash) {
        if (plainText == null) {
            return false;
        }
        return encoder.matches(plainText, hash.value());
    }
}
