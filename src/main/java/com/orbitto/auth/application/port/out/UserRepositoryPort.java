package com.orbitto.auth.application.port.out;

import com.orbitto.auth.domain.identity.Email;
import com.orbitto.auth.domain.identity.User;
import com.orbitto.auth.domain.identity.UserId;

import java.util.Optional;

public interface UserRepositoryPort {

    Optional<User> findByEmail(Email email);

    Optional<User> findById(UserId id);

    boolean existsByEmail(Email email);

    void save(User user);
}
