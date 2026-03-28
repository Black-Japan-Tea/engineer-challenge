package com.orbitto.auth.application.port.out;

public interface SecureRandomTokenPort {

    String nextOpaqueToken(int numBytes);
}
