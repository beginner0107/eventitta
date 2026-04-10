package com.eventitta.api.auth.controller;

import com.eventitta.api.auth.controller.request.SignInRequest;
import com.eventitta.api.auth.controller.request.SignUpRequest;
import com.eventitta.api.auth.controller.request.SocialLoginRequest;
import com.eventitta.api.auth.controller.response.SignUpResponse;
import com.eventitta.domain.auth.dto.*;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface AuthMapper {

    SignUpCommand toSignUpCommand(SignUpRequest request);

    default SignInCommand toSignInCommand(SignInRequest request) {
        return toSignInCommand(request, null);
    }

    default SignInCommand toSignInCommand(SignInRequest request, ClientSessionMetadata sessionMetadata) {
        return new SignInCommand(request.email(), request.password(), sessionMetadata);
    }

    SignUpResponse toSignUpResponse(SignUpResult result);

    default KakaoLoginCommand toKakaoLoginCommand(SocialLoginRequest request) {
        return toKakaoLoginCommand(request, null);
    }

    default KakaoLoginCommand toKakaoLoginCommand(SocialLoginRequest request, ClientSessionMetadata sessionMetadata) {
        return new KakaoLoginCommand(request.code(), request.redirectUri(), sessionMetadata);
    }

    KakaoLinkCommand toKakaoLinkCommand(SocialLoginRequest request);

    default RefreshCommand toRefreshCommand(String accessToken, String refreshToken) {
        return toRefreshCommand(accessToken, refreshToken, null);
    }

    default RefreshCommand toRefreshCommand(String accessToken, String refreshToken, ClientSessionMetadata sessionMetadata) {
        return new RefreshCommand(accessToken, refreshToken, sessionMetadata);
    }

    default LogoutCommand toLogoutCommand(String accessToken, String refreshToken) {
        return new LogoutCommand(accessToken, refreshToken);
    }

    default TokenResult toTokenResult(String accessToken, String refreshToken) {
        return new TokenResult(accessToken, refreshToken);
    }
}
