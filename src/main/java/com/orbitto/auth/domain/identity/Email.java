package com.orbitto.auth.domain.identity;

import com.orbitto.auth.domain.shared.DomainException;

import java.util.Locale;
import java.util.regex.Pattern;

/** Email: нормализация в lower-case и ограничение длины (практический потолок под хранение). */
public record Email(String value) {

    private static final int MAX = 320;
    private static final Pattern BASIC =
            Pattern.compile("^[a-zA-Z0-9_+&*-]+(?:\\.[a-zA-Z0-9_+&*-]+)*@(?:[a-zA-Z0-9-]+\\.)+[a-zA-Z]{2,}$");

    public Email {
        if (value == null || value.isBlank()) {
            throw new DomainException("email.required");
        }
        String trimmed = value.trim();
        if (trimmed.length() > MAX) {
            throw new DomainException("email.too_long");
        }
        if (!BASIC.matcher(trimmed).matches()) {
            throw new DomainException("email.invalid_format");
        }
        value = trimmed.toLowerCase(Locale.ROOT);
    }

    public static Email of(String raw) {
        return new Email(raw);
    }
}
