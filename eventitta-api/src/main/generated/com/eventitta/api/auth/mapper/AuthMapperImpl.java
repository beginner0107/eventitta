package com.eventitta.api.auth.mapper;

import com.eventitta.api.auth.controller.request.SignUpRequest;
import com.eventitta.api.auth.controller.request.SocialLoginRequest;
import com.eventitta.api.auth.controller.response.SignUpResponse;
import com.eventitta.domain.auth.service.dto.KakaoLinkCommand;
import com.eventitta.domain.auth.service.dto.SignUpCommand;
import com.eventitta.domain.auth.service.dto.SignUpResult;
import javax.annotation.processing.Generated;
import org.springframework.stereotype.Component;

@Generated(
    value = "org.mapstruct.ap.MappingProcessor",
    date = "2026-03-27T20:34:57+0900",
    comments = "version: 1.5.5.Final, compiler: javac, environment: Java 17.0.16 (Amazon.com Inc.)"
)
@Component
public class AuthMapperImpl implements AuthMapper {

    @Override
    public SignUpCommand toSignUpCommand(SignUpRequest request) {
        if ( request == null ) {
            return null;
        }

        String email = null;
        String password = null;
        String nickname = null;

        email = request.email();
        password = request.password();
        nickname = request.nickname();

        SignUpCommand signUpCommand = new SignUpCommand( email, password, nickname );

        return signUpCommand;
    }

    @Override
    public SignUpResponse toSignUpResponse(SignUpResult result) {
        if ( result == null ) {
            return null;
        }

        String email = null;
        String nickname = null;

        email = result.email();
        nickname = result.nickname();

        SignUpResponse signUpResponse = new SignUpResponse( email, nickname );

        return signUpResponse;
    }

    @Override
    public KakaoLinkCommand toKakaoLinkCommand(SocialLoginRequest request) {
        if ( request == null ) {
            return null;
        }

        String code = null;
        String redirectUri = null;

        code = request.code();
        redirectUri = request.redirectUri();

        KakaoLinkCommand kakaoLinkCommand = new KakaoLinkCommand( code, redirectUri );

        return kakaoLinkCommand;
    }
}
