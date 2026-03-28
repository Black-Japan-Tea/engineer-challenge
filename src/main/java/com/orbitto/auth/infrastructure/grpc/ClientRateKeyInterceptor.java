package com.orbitto.auth.infrastructure.grpc;

import io.grpc.Context;
import io.grpc.Contexts;
import io.grpc.Grpc;
import io.grpc.Metadata;
import io.grpc.ServerCall;
import io.grpc.ServerCallHandler;
import io.grpc.ServerInterceptor;
import net.devh.boot.grpc.server.interceptor.GrpcGlobalServerInterceptor;
import org.springframework.stereotype.Component;

/** Кладёт в Context ключ для лимита регистраций: первый hop из X-Forwarded-For или адрес пира. */
@Component
@GrpcGlobalServerInterceptor
public class ClientRateKeyInterceptor implements ServerInterceptor {

    public static final Metadata.Key<String> X_FORWARDED_FOR =
            Metadata.Key.of("x-forwarded-for", Metadata.ASCII_STRING_MARSHALLER);

    public static final Context.Key<String> CLIENT_RATE_KEY = Context.key("client-rate-key");

    @Override
    public <ReqT, RespT> ServerCall.Listener<ReqT> interceptCall(
            ServerCall<ReqT, RespT> call,
            Metadata headers,
            ServerCallHandler<ReqT, RespT> next) {
        String forwarded = headers.get(X_FORWARDED_FOR);
        Object remote = call.getAttributes().get(Grpc.TRANSPORT_ATTR_REMOTE_ADDR);
        String peer = remote != null ? remote.toString() : "unknown";
        String key = (forwarded != null && !forwarded.isBlank())
                ? forwarded.split(",")[0].trim()
                : peer;
        Context ctx = Context.current().withValue(CLIENT_RATE_KEY, key);
        return Contexts.interceptCall(ctx, call, headers, next);
    }
}
