package com.orbitto.auth.domain.identity;

import com.orbitto.auth.domain.shared.DomainException;

/**
 * Пароль в открытом виде на входе: проверяем до хеширования, в персистентность не идём.
 * Правила ниже — только для регистрации / смены пароля, не для логина.
 */
public record PlainPassword(String value) {

    private static final int MIN = 10;
    private static final int MAX = 128;

    public PlainPassword {
        if (value == null || value.length() < MIN) {
            throw new DomainException("password.too_short", MIN);
        }
        if (value.length() > MAX) {
            throw new DomainException("password.too_long", MAX);
        }
        boolean hasLetter = value.chars().anyMatch(Character::isLetter);
        boolean hasDigit = value.chars().anyMatch(Character::isDigit);
        if (!hasLetter || !hasDigit) {
            throw new DomainException("password.requires_letter_and_digit");
        }
    }

    public static PlainPassword of(String raw) {
        return new PlainPassword(raw);
    }
}
