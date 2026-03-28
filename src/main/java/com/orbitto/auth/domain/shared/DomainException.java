package com.orbitto.auth.domain.shared;

public class DomainException extends RuntimeException {

    private final String code;
    private final Object[] args;

    public DomainException(String code, Object... args) {
        super(code);
        this.code = code;
        this.args = args == null ? new Object[0] : args;
    }

    public String code() {
        return code;
    }

    public Object[] args() {
        return args;
    }
}
