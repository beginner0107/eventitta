package com.eventitta.api.user.mapper;

import com.eventitta.api.user.controller.request.ChangePasswordRequest;
import com.eventitta.api.user.controller.request.UpdateProfileRequest;
import com.eventitta.api.user.controller.response.UserProfileResponse;
import com.eventitta.domain.user.service.dto.ChangePasswordCommand;
import com.eventitta.domain.user.service.dto.UpdateProfileCommand;
import com.eventitta.domain.user.service.dto.UserProfileResult;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface UserMapper {

    UpdateProfileCommand toUpdateProfileCommand(UpdateProfileRequest request);

    ChangePasswordCommand toChangePasswordCommand(ChangePasswordRequest request);

    UserProfileResponse toUserProfileResponse(UserProfileResult result);
}
