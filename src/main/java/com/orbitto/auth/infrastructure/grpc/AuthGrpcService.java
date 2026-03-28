package com.orbitto.auth.infrastructure.grpc;

import com.orbitto.auth.application.command.ConfirmPasswordResetCommand;
import com.orbitto.auth.application.command.ConfirmPasswordResetHandler;
import com.orbitto.auth.application.command.LoginCommand;
import com.orbitto.auth.application.command.LoginHandler;
import com.orbitto.auth.application.command.RefreshTokenCommand;
import com.orbitto.auth.application.command.RefreshTokenHandler;
import com.orbitto.auth.application.command.RegisterUserCommand;
import com.orbitto.auth.application.command.RegisterUserHandler;
import com.orbitto.auth.application.command.RequestPasswordResetCommand;
import com.orbitto.auth.application.command.RequestPasswordResetHandler;
import com.orbitto.auth.grpc.v1.AuthServiceGrpc;
import com.orbitto.auth.grpc.v1.ConfirmPasswordResetRequest;
import com.orbitto.auth.grpc.v1.ConfirmPasswordResetResponse;
import com.orbitto.auth.grpc.v1.LoginRequest;
import com.orbitto.auth.grpc.v1.RefreshTokenRequest;
import com.orbitto.auth.grpc.v1.RegisterRequest;
import com.orbitto.auth.grpc.v1.RegisterResponse;
import com.orbitto.auth.grpc.v1.RequestPasswordResetRequest;
import com.orbitto.auth.grpc.v1.RequestPasswordResetResponse;
import com.orbitto.auth.grpc.v1.TokenPairResponse;
import io.grpc.Context;
import io.grpc.stub.StreamObserver;
import net.devh.boot.grpc.server.service.GrpcService;

@GrpcService
public class AuthGrpcService extends AuthServiceGrpc.AuthServiceImplBase {

    private final RegisterUserHandler registerUserHandler;
    private final LoginHandler loginHandler;
    private final RefreshTokenHandler refreshTokenHandler;
    private final RequestPasswordResetHandler requestPasswordResetHandler;
    private final ConfirmPasswordResetHandler confirmPasswordResetHandler;

    public AuthGrpcService(RegisterUserHandler registerUserHandler,
                           LoginHandler loginHandler,
                           RefreshTokenHandler refreshTokenHandler,
                           RequestPasswordResetHandler requestPasswordResetHandler,
                           ConfirmPasswordResetHandler confirmPasswordResetHandler) {
        this.registerUserHandler = registerUserHandler;
        this.loginHandler = loginHandler;
        this.refreshTokenHandler = refreshTokenHandler;
        this.requestPasswordResetHandler = requestPasswordResetHandler;
        this.confirmPasswordResetHandler = confirmPasswordResetHandler;
    }

    @Override
    public void register(RegisterRequest request, StreamObserver<RegisterResponse> responseObserver) {
        try {
            String clientKey = ClientRateKeyInterceptor.CLIENT_RATE_KEY.get(Context.current());
            var id = registerUserHandler.handle(new RegisterUserCommand(
                    request.getEmail(),
                    request.getPassword(),
                    clientKey != null ? clientKey : "anonymous"
            ));
            responseObserver.onNext(RegisterResponse.newBuilder().setUserId(id.value().toString()).build());
            responseObserver.onCompleted();
        } catch (Exception e) {
            responseObserver.onError(GrpcStatusMapper.map(e));
        }
    }

    @Override
    public void login(LoginRequest request, StreamObserver<TokenPairResponse> responseObserver) {
        try {
            var pair = loginHandler.handle(new LoginCommand(request.getEmail(), request.getPassword()));
            responseObserver.onNext(toTokenPair(pair));
            responseObserver.onCompleted();
        } catch (Exception e) {
            responseObserver.onError(GrpcStatusMapper.map(e));
        }
    }

    @Override
    public void refreshToken(RefreshTokenRequest request, StreamObserver<TokenPairResponse> responseObserver) {
        try {
            var pair = refreshTokenHandler.handle(new RefreshTokenCommand(request.getRefreshToken()));
            responseObserver.onNext(toTokenPair(pair));
            responseObserver.onCompleted();
        } catch (Exception e) {
            responseObserver.onError(GrpcStatusMapper.map(e));
        }
    }

    @Override
    public void requestPasswordReset(RequestPasswordResetRequest request,
                                     StreamObserver<RequestPasswordResetResponse> responseObserver) {
        try {
            requestPasswordResetHandler.handle(new RequestPasswordResetCommand(request.getEmail()));
            responseObserver.onNext(RequestPasswordResetResponse.newBuilder().setAcknowledged(true).build());
            responseObserver.onCompleted();
        } catch (Exception e) {
            responseObserver.onError(GrpcStatusMapper.map(e));
        }
    }

    @Override
    public void confirmPasswordReset(ConfirmPasswordResetRequest request,
                                     StreamObserver<ConfirmPasswordResetResponse> responseObserver) {
        try {
            confirmPasswordResetHandler.handle(
                    new ConfirmPasswordResetCommand(request.getToken(), request.getNewPassword()));
            responseObserver.onNext(ConfirmPasswordResetResponse.newBuilder().setSuccess(true).build());
            responseObserver.onCompleted();
        } catch (Exception e) {
            responseObserver.onError(GrpcStatusMapper.map(e));
        }
    }

    private static TokenPairResponse toTokenPair(com.orbitto.auth.application.shared.TokenPair pair) {
        return TokenPairResponse.newBuilder()
                .setAccessToken(pair.accessToken())
                .setRefreshToken(pair.refreshToken())
                .setAccessExpiresAtEpochSeconds(pair.accessExpiresAt().getEpochSecond())
                .setRefreshExpiresAtEpochSeconds(pair.refreshExpiresAt().getEpochSecond())
                .build();
    }
}
