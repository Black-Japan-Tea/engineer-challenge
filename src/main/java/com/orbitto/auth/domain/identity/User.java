package com.orbitto.auth.domain.identity;

import java.time.Instant;

/** Агрегат «зарегистрированный пользователь» в контексте идентичности. */
public final class User {

    private final UserId id;
    private final Email email;
    private PasswordHash passwordHash;
    private final Instant createdAt;
    private Instant updatedAt;

    private User(UserId id, Email email, PasswordHash passwordHash, Instant createdAt, Instant updatedAt) {
        this.id = id;
        this.email = email;
        this.passwordHash = passwordHash;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public static User register(UserId id, Email email, PasswordHash passwordHash, Instant now) {
        return new User(id, email, passwordHash, now, now);
    }

    public static User rehydrate(UserId id, Email email, PasswordHash passwordHash,
                                 Instant createdAt, Instant updatedAt) {
        return new User(id, email, passwordHash, createdAt, updatedAt);
    }

    public void changePassword(PasswordHash newHash, Instant now) {
        this.passwordHash = newHash;
        this.updatedAt = now;
    }

    public UserId id() {
        return id;
    }

    public Email email() {
        return email;
    }

    public PasswordHash passwordHash() {
        return passwordHash;
    }

    public Instant createdAt() {
        return createdAt;
    }

    public Instant updatedAt() {
        return updatedAt;
    }
}
