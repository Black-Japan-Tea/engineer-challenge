package com.orbitto.auth.application.command;

public record ConfirmPasswordResetCommand(String token, String newPassword) {}
