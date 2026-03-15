package com.eventitta.auth.mapper;

import com.eventitta.auth.controller.request.SignInRequest;
import com.eventitta.auth.controller.request.SignUpRequest;
import com.eventitta.auth.controller.response.SignUpResponse;
import com.eventitta.auth.service.dto.LogoutCommand;
import com.eventitta.auth.service.dto.RefreshCommand;
import com.eventitta.auth.service.dto.SignInCommand;
import com.eventitta.auth.service.dto.SignUpCommand;
import com.eventitta.auth.service.dto.SignUpResult;
import com.eventitta.auth.service.dto.TokenResult;
import com.eventitta.user.domain.User;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface AuthMapper {

    SignUpCommand toSignUpCommand(SignUpRequest request);

    SignInCommand toSignInCommand(SignInRequest request);

    SignUpResult toSignUpResult(User user);

    SignUpResponse toSignUpResponse(SignUpResult result);

    default RefreshCommand toRefreshCommand(String accessToken, String refreshToken) {
        return new RefreshCommand(accessToken, refreshToken);
    }

    default LogoutCommand toLogoutCommand(String accessToken, String refreshToken) {
        return new LogoutCommand(accessToken, refreshToken);
    }

    default TokenResult toTokenResult(String accessToken, String refreshToken) {
        return new TokenResult(accessToken, refreshToken);
    }
}
