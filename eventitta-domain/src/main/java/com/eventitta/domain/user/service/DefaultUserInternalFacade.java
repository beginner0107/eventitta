package com.eventitta.domain.user.service;

import com.eventitta.domain.user.api.internal.command.RegisterLocalUserCommand;
import com.eventitta.domain.user.api.internal.command.RegisterSocialUserCommand;
import com.eventitta.domain.user.api.internal.facade.UserInternalFacade;
import com.eventitta.domain.user.api.internal.result.RegisteredUserResult;
import com.eventitta.domain.user.api.internal.view.UserAuditView;
import com.eventitta.domain.user.api.internal.view.UserAuthView;
import com.eventitta.domain.user.api.internal.view.UserProfileView;
import com.eventitta.domain.user.domain.Provider;
import com.eventitta.domain.user.domain.Role;
import com.eventitta.domain.user.domain.User;
import com.eventitta.domain.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.regex.Pattern;

import static com.eventitta.domain.user.exception.UserErrorCode.CONFLICTED_EMAIL;
import static com.eventitta.domain.user.exception.UserErrorCode.CONFLICTED_NICKNAME;
import static com.eventitta.domain.user.exception.UserErrorCode.NOT_FOUND_USER_ID;

@Component
@RequiredArgsConstructor
class DefaultUserInternalFacade implements UserInternalFacade {
    private static final int NICKNAME_MAX_LENGTH = 20;
    private static final int NICKNAME_SUFFIX_LENGTH = 4;
    private static final Pattern NICKNAME_ALLOWED_PATTERN = Pattern.compile("[^가-힣a-zA-Z0-9]");

    private final UserRepository userRepository;

    @Override
    @Transactional
    public RegisteredUserResult registerLocalUser(RegisterLocalUserCommand command) {
        if (userRepository.existsByEmail(command.email())) {
            throw CONFLICTED_EMAIL.defaultException();
        }
        if (userRepository.existsByNickname(command.nickname())) {
            throw CONFLICTED_NICKNAME.defaultException();
        }

        User user = User.builder()
            .email(command.email())
            .password(command.encodedPassword())
            .emailVerified(false)
            .nickname(command.nickname())
            .role(Role.USER)
            .provider(Provider.LOCAL)
            .build();

        try {
            User savedUser = userRepository.saveAndFlush(user);
            return new RegisteredUserResult(savedUser.getId(), savedUser.getEmail(), savedUser.getNickname());
        } catch (DataIntegrityViolationException ex) {
            throw mapSignupConflict(ex);
        }
    }

    @Override
    @Transactional
    public RegisteredUserResult registerSocialUser(RegisterSocialUserCommand command) {
        if (userRepository.existsByEmail(command.email())) {
            throw CONFLICTED_EMAIL.defaultException();
        }

        String nickname = resolveUniqueNickname(command.preferredNickname());

        User user = User.builder()
            .email(command.email())
            .password(null)
            .emailVerified(true)
            .nickname(nickname)
            .profilePictureUrl(command.profilePictureUrl())
            .role(Role.USER)
            .provider(resolveProvider(command.provider()))
            .providerId(command.providerId())
            .build();

        try {
            User savedUser = userRepository.saveAndFlush(user);
            return new RegisteredUserResult(savedUser.getId(), savedUser.getEmail(), savedUser.getNickname());
        } catch (DataIntegrityViolationException ex) {
            throw mapSignupConflict(ex);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<UserAuthView> findActiveUserByEmail(String email) {
        return userRepository.findActiveByEmail(email)
            .map(this::toUserAuthView);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<UserAuthView> findActiveUserById(Long userId) {
        return userRepository.findActiveById(userId)
            .map(this::toUserAuthView);
    }

    @Override
    @Transactional
    public UserAuthView markEmailVerified(Long userId) {
        User user = userRepository.findActiveById(userId)
            .orElseThrow(NOT_FOUND_USER_ID::defaultException);
        user.markEmailVerified();
        return toUserAuthView(user);
    }

    @Override
    @Transactional
    public UserAuthView resetPassword(Long userId, String encodedPassword) {
        User user = userRepository.findActiveById(userId)
            .orElseThrow(NOT_FOUND_USER_ID::defaultException);
        user.changePassword(encodedPassword);
        user.bumpAuthVersion();
        return toUserAuthView(user);
    }

    @Override
    @Transactional
    public UserAuthView setLocalPassword(Long userId, String encodedPassword) {
        User user = userRepository.findActiveById(userId)
            .orElseThrow(NOT_FOUND_USER_ID::defaultException);
        user.setLocalPassword(encodedPassword);
        user.markEmailVerified();
        user.bumpAuthVersion();
        return toUserAuthView(user);
    }

    @Override
    @Transactional
    public UserAuthView unlinkKakaoProvider(Long userId) {
        User user = userRepository.findActiveById(userId)
            .orElseThrow(NOT_FOUND_USER_ID::defaultException);
        if (user.getProvider() == Provider.KAKAO) {
            user.unlinkLegacySocialIdentity();
        }
        user.bumpAuthVersion();
        return toUserAuthView(user);
    }

    @Override
    @Transactional
    public UserAuthView bumpAuthVersion(Long userId) {
        User user = userRepository.findActiveById(userId)
            .orElseThrow(NOT_FOUND_USER_ID::defaultException);
        user.bumpAuthVersion();
        return toUserAuthView(user);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<UserAuditView> findUserById(Long userId) {
        return userRepository.findById(userId)
            .map(user -> new UserAuditView(user.getId(), user.getEmail()));
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<UserProfileView> findUserProfile(Long userId) {
        return userRepository.findById(userId)
            .map(this::toUserProfileView);
    }

    @Override
    @Transactional(readOnly = true)
    public Map<Long, UserProfileView> findUserProfiles(Collection<Long> userIds) {
        if (userIds == null || userIds.isEmpty()) {
            return Map.of();
        }

        Map<Long, UserProfileView> results = new LinkedHashMap<>();
        for (User user : userRepository.findAllById(userIds)) {
            results.put(user.getId(), toUserProfileView(user));
        }
        return results;
    }

    @Override
    @Transactional(readOnly = true)
    public void ensureActiveUser(Long userId) {
        if (userRepository.findActiveById(userId).isEmpty()) {
            throw NOT_FOUND_USER_ID.defaultException();
        }
    }

    @Override
    @Transactional(readOnly = true)
    public boolean isProfilePictureReferenced(Long mediaAssetId) {
        return userRepository.existsByProfilePictureMediaId(mediaAssetId);
    }

    private UserAuthView toUserAuthView(User user) {
        return new UserAuthView(
            user.getId(),
            user.getEmail(),
            user.getPassword(),
            user.getRole().name(),
            user.isEmailVerified(),
            user.isSuspended(),
            user.getAuthVersion(),
            user.getNickname(),
            user.getProfilePictureUrl()
        );
    }

    private UserProfileView toUserProfileView(User user) {
        if (user.isDeleted()) {
            return new UserProfileView(user.getId(), "탈퇴한 사용자", null, null, true);
        }
        return new UserProfileView(
            user.getId(),
            user.getNickname(),
            user.getProfilePictureMediaId(),
            user.getProfilePictureUrl(),
            false
        );
    }

    private RuntimeException mapSignupConflict(DataIntegrityViolationException ex) {
        String message = extractExceptionMessage(ex);
        if (message.contains("uk_users_email") || message.contains("users.email")) {
            return CONFLICTED_EMAIL.defaultException(ex);
        }
        if (message.contains("uk_users_nickname") || message.contains("users.nickname")) {
            return CONFLICTED_NICKNAME.defaultException(ex);
        }
        throw ex;
    }

    private Provider resolveProvider(String provider) {
        return Provider.valueOf(provider.toUpperCase(Locale.ROOT));
    }

    private String resolveUniqueNickname(String preferredNickname) {
        String baseNickname = normalizeNickname(preferredNickname);
        if (!userRepository.existsByNickname(baseNickname)) {
            return baseNickname;
        }

        for (int attempt = 1; attempt <= 9999; attempt++) {
            String suffix = String.format(Locale.ROOT, "%04d", attempt);
            int maxBaseLength = Math.max(2, NICKNAME_MAX_LENGTH - suffix.length());
            String truncatedBase = truncate(baseNickname, maxBaseLength);
            String candidate = truncatedBase + suffix;
            if (!userRepository.existsByNickname(candidate)) {
                return candidate;
            }
        }

        return "user" + UUID.randomUUID().toString().replace("-", "").substring(0, 6);
    }

    private String normalizeNickname(String preferredNickname) {
        String sanitized = preferredNickname == null ? "" : NICKNAME_ALLOWED_PATTERN.matcher(preferredNickname).replaceAll("");
        if (sanitized.length() > NICKNAME_MAX_LENGTH) {
            sanitized = sanitized.substring(0, NICKNAME_MAX_LENGTH);
        }
        if (sanitized.length() >= 2) {
            return sanitized;
        }
        return "user" + UUID.randomUUID().toString().replace("-", "").substring(0, 4);
    }

    private String truncate(String nickname, int maxLength) {
        if (nickname.length() <= maxLength) {
            return nickname;
        }
        return nickname.substring(0, maxLength);
    }

    private String extractExceptionMessage(Throwable throwable) {
        StringBuilder builder = new StringBuilder();
        Throwable current = throwable;

        while (current != null) {
            if (current.getMessage() != null) {
                builder.append(current.getMessage()).append(' ');
            }
            current = current.getCause();
        }

        return builder.toString().toLowerCase(Locale.ROOT);
    }
}
