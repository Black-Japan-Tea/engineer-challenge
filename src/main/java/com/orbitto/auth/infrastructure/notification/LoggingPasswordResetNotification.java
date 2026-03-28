package com.orbitto.auth.infrastructure.notification;

import com.orbitto.auth.application.port.out.PasswordResetNotificationPort;
import com.orbitto.auth.domain.identity.Email;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class LoggingPasswordResetNotification implements PasswordResetNotificationPort {

    private static final Logger log = LoggerFactory.getLogger(LoggingPasswordResetNotification.class);

    @Override
    public void sendResetLink(Email email, String rawToken) {
        log.info("Password reset for {} — token (dev only): {}", email.value(), rawToken);
    }
}
