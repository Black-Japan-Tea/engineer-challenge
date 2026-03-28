package com.orbitto.auth.application.command;

public record RegisterUserCommand(String email, String password, String clientKeyForRateLimit) {}
