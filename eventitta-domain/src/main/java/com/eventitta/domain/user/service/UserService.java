package com.eventitta.domain.user.service;

import com.eventitta.domain.auth.api.internal.facade.AuthUserLifecycleFacade;
import com.eventitta.domain.gamification.api.internal.facade.GamificationInternalFacade;
import com.eventitta.domain.media.api.internal.facade.MediaAssetInternalFacade;
import com.eventitta.domain.media.api.internal.view.MediaAttachmentView;
import com.eventitta.domain.media.domain.MediaCategory;
import com.eventitta.domain.meeting.api.internal.facade.MeetingUserLifecycleFacade;
import com.eventitta.domain.user.domain.User;
import com.eventitta.domain.user.repository.UserRepository;
import com.eventitta.domain.user.service.dto.ChangePasswordCommand;
import com.eventitta.domain.user.service.dto.UpdateProfileCommand;
import com.eventitta.domain.user.service.dto.UserProfileResult;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import static com.eventitta.domain.user.exception.UserErrorCode.*;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserService {
    private final UserRepository userRepository;
    private final AuthUserLifecycleFacade authUserLifecycleFacade;
    private final PasswordEncoder passwordEncoder;
    private final MeetingUserLifecycleFacade meetingUserLifecycleFacade;
    private final GamificationInternalFacade gamificationFacade;
    private final MediaAssetInternalFacade mediaAssetInternalFacade;

    public UserProfileResult getProfile(Long userId) {
        User user = userRepository.findActiveById(userId)
            .orElseThrow(NOT_FOUND_USER_ID::defaultException);
        String profilePictureUrl = user.getProfilePictureMediaId() != null
            ? mediaAssetInternalFacade.resolveDisplayUrl(user.getProfilePictureMediaId())
            : user.getProfilePictureUrl();
        return new UserProfileResult(
            user.getId(),
            user.getEmail(),
            user.getNickname(),
            profilePictureUrl,
            user.getSelfIntro(),
            user.getInterests(),
            user.getAddress(),
            user.getLatitude(),
            user.getLongitude()
        );
    }

    @Transactional
    public void updateProfile(Long userId, UpdateProfileCommand command) {
        User user = userRepository.findActiveById(userId)
            .orElseThrow(NOT_FOUND_USER_ID::defaultException);
        if (!user.getNickname().equals(command.nickname()) &&
            userRepository.existsByNickname(command.nickname())) {
            throw CONFLICTED_NICKNAME.defaultException();
        }

        Long previousMediaId = user.getProfilePictureMediaId();
        MediaAttachmentView profileAsset = null;
        String profilePictureUrl = null;
        if (command.profileImageMediaId() != null) {
            profileAsset = mediaAssetInternalFacade.resolveAndAttachAssets(
                userId,
                MediaCategory.PROFILE_IMAGE,
                java.util.List.of(command.profileImageMediaId())
            ).get(0);
            profilePictureUrl = profileAsset.publicUrl();
        }

        user.updateProfile(
            command.nickname(),
            profileAsset != null ? profileAsset.mediaId() : null,
            profilePictureUrl,
            command.selfIntro(),
            command.interests(),
            command.address(),
            command.latitude(),
            command.longitude()
        );
        userRepository.flush();
        if (previousMediaId != null
            && (profileAsset == null || !previousMediaId.equals(profileAsset.mediaId()))
        ) {
            mediaAssetInternalFacade.releaseAssetsIfUnreferenced(java.util.List.of(previousMediaId));
        }
    }

    @Transactional
    public void deleteUser(Long userId) {
        User user = userRepository.findActiveById(userId)
            .orElseThrow(NOT_FOUND_USER_ID::defaultException);
        Long previousProfileMediaId = user.getProfilePictureMediaId();
        meetingUserLifecycleFacade.removeUserFromActiveMeetings(userId);
        gamificationFacade.removeUserData(userId);
        user.bumpAuthVersion();
        user.delete();
        authUserLifecycleFacade.cleanupDeletedUser(userId);
        userRepository.flush();
        if (previousProfileMediaId != null) {
            mediaAssetInternalFacade.releaseAssetsIfUnreferenced(java.util.List.of(previousProfileMediaId));
        }
    }

    @Transactional
    public void changePassword(Long userId, ChangePasswordCommand command) {
        User user = userRepository.findActiveById(userId)
            .orElseThrow(NOT_FOUND_USER_ID::defaultException);
        if (user.getPassword() == null || !passwordEncoder.matches(command.currentPassword(), user.getPassword())) {
            throw INVALID_CURRENT_PASSWORD.defaultException();
        }
        user.changePassword(passwordEncoder.encode(command.newPassword()));
        user.bumpAuthVersion();
        authUserLifecycleFacade.revokeAllSessions(userId);
    }
}
