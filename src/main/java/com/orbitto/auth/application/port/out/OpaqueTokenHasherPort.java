package com.orbitto.auth.application.port.out;

public interface OpaqueTokenHasherPort {

    String hash(String rawToken);
}
