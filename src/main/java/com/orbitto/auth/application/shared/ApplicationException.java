package com.orbitto.auth.application.shared;

public class ApplicationException extends RuntimeException {

    private final String code;

    public ApplicationException(String code) {
        super(code);
        this.code = code;
    }

    public ApplicationException(String code, Throwable cause) {
        super(code, cause);
        this.code = code;
    }

    public String code() {
        return code;
    }
}
