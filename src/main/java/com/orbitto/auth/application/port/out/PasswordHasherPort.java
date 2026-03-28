package com.orbitto.auth.application.port.out;

import com.orbitto.auth.domain.identity.PasswordHash;
import com.orbitto.auth.domain.identity.PlainPassword;

public interface PasswordHasherPort {

    PasswordHash hash(PlainPassword plain);

    boolean matches(PlainPassword plain, PasswordHash hash);

    /** Логин: проверка «как есть», без правил длины/сложности как при регистрации. */
    boolean matchesRaw(String plainText, PasswordHash hash);
}
