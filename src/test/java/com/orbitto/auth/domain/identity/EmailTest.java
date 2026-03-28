package com.orbitto.auth.domain.identity;

import com.orbitto.auth.domain.shared.DomainException;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class EmailTest {

    @Test
    void normalizesCase() {
        assertThat(Email.of("User@Example.COM").value()).isEqualTo("user@example.com");
    }

    @Test
    void rejectsInvalid() {
        assertThatThrownBy(() -> Email.of("not-an-email"))
                .isInstanceOfSatisfying(DomainException.class,
                        ex -> assertThat(ex.code()).isEqualTo("email.invalid_format"));
    }
}
