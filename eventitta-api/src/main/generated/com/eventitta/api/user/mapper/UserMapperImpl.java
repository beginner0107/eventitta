package com.eventitta.api.user.mapper;

import com.eventitta.api.user.controller.request.ChangePasswordRequest;
import com.eventitta.api.user.controller.request.UpdateProfileRequest;
import com.eventitta.api.user.controller.response.UserProfileResponse;
import com.eventitta.domain.user.domain.User;
import com.eventitta.domain.user.service.dto.ChangePasswordCommand;
import com.eventitta.domain.user.service.dto.UpdateProfileCommand;
import com.eventitta.domain.user.service.dto.UserProfileResult;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import javax.annotation.processing.Generated;
import org.springframework.stereotype.Component;

@Generated(
    value = "org.mapstruct.ap.MappingProcessor",
    date = "2026-03-27T20:34:57+0900",
    comments = "version: 1.5.5.Final, compiler: javac, environment: Java 17.0.16 (Amazon.com Inc.)"
)
@Component
public class UserMapperImpl implements UserMapper {

    @Override
    public UpdateProfileCommand toUpdateProfileCommand(UpdateProfileRequest request) {
        if ( request == null ) {
            return null;
        }

        String nickname = null;
        Long profileImageMediaId = null;
        String selfIntro = null;
        List<String> interests = null;
        String address = null;
        BigDecimal latitude = null;
        BigDecimal longitude = null;

        nickname = request.nickname();
        profileImageMediaId = request.profileImageMediaId();
        selfIntro = request.selfIntro();
        List<String> list = request.interests();
        if ( list != null ) {
            interests = new ArrayList<String>( list );
        }
        address = request.address();
        latitude = request.latitude();
        longitude = request.longitude();

        UpdateProfileCommand updateProfileCommand = new UpdateProfileCommand( nickname, profileImageMediaId, selfIntro, interests, address, latitude, longitude );

        return updateProfileCommand;
    }

    @Override
    public ChangePasswordCommand toChangePasswordCommand(ChangePasswordRequest request) {
        if ( request == null ) {
            return null;
        }

        String currentPassword = null;
        String newPassword = null;

        currentPassword = request.currentPassword();
        newPassword = request.newPassword();

        ChangePasswordCommand changePasswordCommand = new ChangePasswordCommand( currentPassword, newPassword );

        return changePasswordCommand;
    }

    @Override
    public UserProfileResult toUserProfileResult(User user) {
        if ( user == null ) {
            return null;
        }

        Long id = null;
        String email = null;
        String nickname = null;
        String profilePictureUrl = null;
        String selfIntro = null;
        List<String> interests = null;
        String address = null;
        BigDecimal latitude = null;
        BigDecimal longitude = null;

        id = user.getId();
        email = user.getEmail();
        nickname = user.getNickname();
        profilePictureUrl = user.getProfilePictureUrl();
        selfIntro = user.getSelfIntro();
        List<String> list = user.getInterests();
        if ( list != null ) {
            interests = new ArrayList<String>( list );
        }
        address = user.getAddress();
        latitude = user.getLatitude();
        longitude = user.getLongitude();

        UserProfileResult userProfileResult = new UserProfileResult( id, email, nickname, profilePictureUrl, selfIntro, interests, address, latitude, longitude );

        return userProfileResult;
    }

    @Override
    public UserProfileResponse toUserProfileResponse(UserProfileResult result) {
        if ( result == null ) {
            return null;
        }

        Long id = null;
        String email = null;
        String nickname = null;
        String profilePictureUrl = null;
        String selfIntro = null;
        List<String> interests = null;
        String address = null;
        BigDecimal latitude = null;
        BigDecimal longitude = null;

        id = result.id();
        email = result.email();
        nickname = result.nickname();
        profilePictureUrl = result.profilePictureUrl();
        selfIntro = result.selfIntro();
        List<String> list = result.interests();
        if ( list != null ) {
            interests = new ArrayList<String>( list );
        }
        address = result.address();
        latitude = result.latitude();
        longitude = result.longitude();

        UserProfileResponse userProfileResponse = new UserProfileResponse( id, email, nickname, profilePictureUrl, selfIntro, interests, address, latitude, longitude );

        return userProfileResponse;
    }
}
