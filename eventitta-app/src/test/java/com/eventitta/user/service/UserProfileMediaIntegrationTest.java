package com.eventitta.domain.user.service;

import com.eventitta.IntegrationTestSupport;
import com.eventitta.domain.media.domain.MediaAsset;
import com.eventitta.domain.media.domain.MediaAssetStatus;
import com.eventitta.domain.media.domain.MediaCategory;
import com.eventitta.domain.media.domain.MediaStorageProvider;
import com.eventitta.domain.media.repository.MediaAssetRepository;
import com.eventitta.domain.user.domain.Provider;
import com.eventitta.domain.user.domain.Role;
import com.eventitta.domain.user.domain.User;
import com.eventitta.domain.user.repository.UserRepository;
import com.eventitta.domain.user.service.dto.UpdateProfileCommand;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@Transactional
@DisplayName("프로필 미디어 라이프사이클 통합 테스트")
class UserProfileMediaIntegrationTest extends IntegrationTestSupport {

    @Autowired
    private UserService userService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private MediaAssetRepository mediaAssetRepository;

    @Test
    @DisplayName("프로필 이미지를 교체하면 이전 자산은 RELEASED 되고 새 자산은 ATTACHED 된다")
    void updateProfile_releasesPreviousAssetAndAttachesNewOne() {
        User user = userRepository.saveAndFlush(createUser("profile-media@test.com", "profileMedia"));
        MediaAsset firstAsset = mediaAssetRepository.saveAndFlush(createTempAsset(user, "first"));
        MediaAsset secondAsset = mediaAssetRepository.saveAndFlush(createTempAsset(user, "second"));

        userService.updateProfile(user.getId(), new UpdateProfileCommand(
            user.getNickname(),
            firstAsset.getId(),
            "hello",
            List.of("music"),
            "Seoul",
            null,
            null
        ));
        userService.updateProfile(user.getId(), new UpdateProfileCommand(
            user.getNickname(),
            secondAsset.getId(),
            "hello again",
            List.of("travel"),
            "Busan",
            null,
            null
        ));

        User persistedUser = userRepository.findActiveById(user.getId()).orElseThrow();
        MediaAsset persistedFirstAsset = mediaAssetRepository.findById(firstAsset.getId()).orElseThrow();
        MediaAsset persistedSecondAsset = mediaAssetRepository.findById(secondAsset.getId()).orElseThrow();

        assertThat(persistedUser.getProfilePictureMediaId()).isEqualTo(secondAsset.getId());
        assertThat(persistedUser.getProfilePictureUrl()).isEqualTo(secondAsset.getPublicUrl());
        assertThat(persistedFirstAsset.getStatus()).isEqualTo(MediaAssetStatus.RELEASED);
        assertThat(persistedSecondAsset.getStatus()).isEqualTo(MediaAssetStatus.ATTACHED);
    }

    private User createUser(String email, String nickname) {
        return User.builder()
            .email(email)
            .password("encoded-password")
            .nickname(nickname)
            .provider(Provider.LOCAL)
            .role(Role.USER)
            .build();
    }

    private MediaAsset createTempAsset(User owner, String suffix) {
        return MediaAsset.temp(
            owner.getId(),
            MediaCategory.PROFILE_IMAGE,
            MediaStorageProvider.LOCAL,
            "profile-image/" + owner.getId() + "/asset-" + suffix + ".png",
            "/api/v1/uploads/profile-image/" + owner.getId() + "/asset-" + suffix + ".png",
            "asset-" + suffix + ".png",
            "image/png",
            100L,
            "checksum-" + suffix,
            1,
            1
        );
    }
}
