package com.orbitto.auth.infrastructure.persistence;

import com.orbitto.auth.application.port.out.UserRepositoryPort;
import com.orbitto.auth.domain.identity.Email;
import com.orbitto.auth.domain.identity.PasswordHash;
import com.orbitto.auth.domain.identity.User;
import com.orbitto.auth.domain.identity.UserId;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
public class UserRepositoryAdapter implements UserRepositoryPort {

    private final UserJpaRepository jpa;

    public UserRepositoryAdapter(UserJpaRepository jpa) {
        this.jpa = jpa;
    }

    @Override
    public Optional<User> findByEmail(Email email) {
        return jpa.findByEmailIgnoreCase(email.value()).map(this::toDomain);
    }

    @Override
    public Optional<User> findById(UserId id) {
        return jpa.findById(id.value()).map(this::toDomain);
    }

    @Override
    public boolean existsByEmail(Email email) {
        return jpa.existsByEmailIgnoreCase(email.value());
    }

    @Override
    public void save(User user) {
        UserEntity e = jpa.findById(user.id().value()).orElseGet(UserEntity::new);
        e.setId(user.id().value());
        e.setEmail(user.email().value());
        e.setPasswordHash(user.passwordHash().value());
        e.setCreatedAt(user.createdAt());
        e.setUpdatedAt(user.updatedAt());
        jpa.save(e);
    }

    private User toDomain(UserEntity e) {
        return User.rehydrate(
                UserId.of(e.getId()),
                Email.of(e.getEmail()),
                new PasswordHash(e.getPasswordHash()),
                e.getCreatedAt(),
                e.getUpdatedAt()
        );
    }
}
