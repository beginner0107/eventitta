package com.eventitta.user.mapper;

import com.eventitta.user.controller.request.ChangePasswordRequest;
import com.eventitta.user.controller.request.UpdateProfileRequest;
import com.eventitta.user.controller.response.UserProfileResponse;
import com.eventitta.user.domain.User;
import com.eventitta.user.service.dto.ChangePasswordCommand;
import com.eventitta.user.service.dto.UpdateProfileCommand;
import com.eventitta.user.service.dto.UserProfileResult;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface UserMapper {

    UpdateProfileCommand toUpdateProfileCommand(UpdateProfileRequest request);

    ChangePasswordCommand toChangePasswordCommand(ChangePasswordRequest request);

    UserProfileResult toUserProfileResult(User user);

    UserProfileResponse toUserProfileResponse(UserProfileResult result);
}
