package com.orbitto.auth.infrastructure.grpc;

import com.orbitto.auth.application.shared.ApplicationException;
import com.orbitto.auth.domain.shared.DomainException;
import io.grpc.Status;
import io.grpc.StatusRuntimeException;

public final class GrpcStatusMapper {

    private GrpcStatusMapper() {}

    public static StatusRuntimeException map(Throwable t) {
        if (t instanceof ApplicationException ae) {
            return switch (ae.code()) {
                case "email_already_registered" -> Status.ALREADY_EXISTS.withDescription(ae.code()).asRuntimeException();
                case "rate_limited" -> Status.RESOURCE_EXHAUSTED.withDescription(ae.code()).asRuntimeException();
                case "invalid_credentials" -> Status.UNAUTHENTICATED.withDescription(ae.code()).asRuntimeException();
                case "invalid_refresh_token" -> Status.UNAUTHENTICATED.withDescription(ae.code()).asRuntimeException();
                case "refresh_token_reuse" -> Status.FAILED_PRECONDITION.withDescription(ae.code()).asRuntimeException();
                case "invalid_reset_token" -> Status.INVALID_ARGUMENT.withDescription(ae.code()).asRuntimeException();
                default -> {
                    if (ae.code() != null && ae.code().startsWith("password.")) {
                        yield Status.INVALID_ARGUMENT.withDescription(ae.code()).asRuntimeException();
                    }
                    yield Status.INVALID_ARGUMENT.withDescription(ae.code()).asRuntimeException();
                }
            };
        }
        if (t instanceof DomainException de) {
            return Status.INVALID_ARGUMENT.withDescription(de.code()).asRuntimeException();
        }
        return Status.INTERNAL.withCause(t).withDescription("internal_error").asRuntimeException();
    }
}
