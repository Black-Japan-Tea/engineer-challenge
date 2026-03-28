package com.orbitto.auth.application.port.out;

import com.orbitto.auth.domain.identity.Email;

public interface PasswordResetNotificationPort {

    void sendResetLink(Email email, String rawToken);
}
