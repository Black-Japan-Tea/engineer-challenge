package com.orbitto.auth.integration;

import com.orbitto.auth.application.command.LoginCommand;
import com.orbitto.auth.application.command.LoginHandler;
import com.orbitto.auth.application.command.RegisterUserCommand;
import com.orbitto.auth.application.command.RegisterUserHandler;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(properties = {
        "grpc.server.port=0",
        "orbitto.auth.jwt.secret=integration-test-secret-at-least-32-chars-long!!"
})
@Testcontainers(disabledWithoutDocker = true)
class AuthStackIntegrationTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine")
            .withDatabaseName("orbitto_test")
            .withUsername("test")
            .withPassword("test");

    @DynamicPropertySource
    static void registerProps(DynamicPropertyRegistry r) {
        r.add("spring.datasource.url", postgres::getJdbcUrl);
        r.add("spring.datasource.username", postgres::getUsername);
        r.add("spring.datasource.password", postgres::getPassword);
    }

    @Autowired
    RegisterUserHandler registerUserHandler;
    @Autowired
    LoginHandler loginHandler;

    @Test
    void registerThenLoginReturnsTokens() {
        var id = registerUserHandler.handle(new RegisterUserCommand(
                "integration@example.com", "mysecurepass1", "test-client"));
        assertThat(id.value()).isNotNull();

        var tokens = loginHandler.handle(new LoginCommand("integration@example.com", "mysecurepass1"));
        assertThat(tokens.accessToken()).isNotBlank();
        assertThat(tokens.refreshToken()).isNotBlank();
    }
}
