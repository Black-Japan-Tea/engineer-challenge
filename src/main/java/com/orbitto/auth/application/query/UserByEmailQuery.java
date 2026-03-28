package com.orbitto.auth.application.query;

import com.orbitto.auth.application.port.out.UserRepositoryPort;
import com.orbitto.auth.domain.identity.Email;
import com.orbitto.auth.domain.identity.User;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

/** Сторона чтения CQRS: пока тот же JPA, что и у команд (без отдельного read-model). */
@Service
public class UserByEmailQuery {

    private final UserRepositoryPort users;

    public UserByEmailQuery(UserRepositoryPort users) {
        this.users = users;
    }

    @Transactional(readOnly = true)
    public Optional<User> find(Email email) {
        return users.findByEmail(email);
    }
}
