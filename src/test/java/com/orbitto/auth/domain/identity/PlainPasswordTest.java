package com.orbitto.auth.domain.identity;

import com.orbitto.auth.domain.shared.DomainException;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PlainPasswordTest {

    @Test
    void acceptsStrongPassword() {
        assertThat(PlainPassword.of("correcthorse1battery").value()).isEqualTo("correcthorse1battery");
    }

    @Test
    void rejectsShort() {
        assertThatThrownBy(() -> PlainPassword.of("short1"))
                .isInstanceOfSatisfying(DomainException.class,
                        ex -> assertThat(ex.code()).isEqualTo("password.too_short"));
    }

    @Test
    void rejectsWithoutDigit() {
        assertThatThrownBy(() -> PlainPassword.of("abcdefghijklmnop"))
                .isInstanceOfSatisfying(DomainException.class,
                        ex -> assertThat(ex.code()).isEqualTo("password.requires_letter_and_digit"));
    }

    @Test
    void rejectsWithoutLetter() {
        assertThatThrownBy(() -> PlainPassword.of("123456789012"))
                .isInstanceOfSatisfying(DomainException.class,
                        ex -> assertThat(ex.code()).isEqualTo("password.requires_letter_and_digit"));
    }
}
