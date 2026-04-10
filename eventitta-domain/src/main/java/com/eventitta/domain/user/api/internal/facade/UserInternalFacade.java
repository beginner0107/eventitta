package com.eventitta.domain.user.api.internal.facade;

import com.eventitta.domain.user.api.internal.command.RegisterLocalUserCommand;
import com.eventitta.domain.user.api.internal.command.RegisterSocialUserCommand;
import com.eventitta.domain.user.api.internal.result.RegisteredUserResult;
import com.eventitta.domain.user.api.internal.view.UserAuditView;
import com.eventitta.domain.user.api.internal.view.UserAuthView;
import com.eventitta.domain.user.api.internal.view.UserProfileView;
import java.util.Collection;
import java.util.Map;
import java.util.Optional;

public interface UserInternalFacade {

    RegisteredUserResult registerLocalUser(RegisterLocalUserCommand command);

    RegisteredUserResult registerSocialUser(RegisterSocialUserCommand command);

    Optional<UserAuthView> findActiveUserByEmail(String email);

    Optional<UserAuthView> findActiveUserById(Long userId);

    UserAuthView markEmailVerified(Long userId);

    UserAuthView resetPassword(Long userId, String encodedPassword);

    UserAuthView setLocalPassword(Long userId, String encodedPassword);

    UserAuthView unlinkKakaoProvider(Long userId);

    UserAuthView bumpAuthVersion(Long userId);

    Optional<UserAuditView> findUserById(Long userId);

    Optional<UserProfileView> findUserProfile(Long userId);

    Map<Long, UserProfileView> findUserProfiles(Collection<Long> userIds);

    void ensureActiveUser(Long userId);

    boolean isProfilePictureReferenced(Long mediaAssetId);
}
