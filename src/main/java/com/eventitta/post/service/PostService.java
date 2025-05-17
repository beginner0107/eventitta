package com.eventitta.post.service;

import com.eventitta.post.domain.Post;
import com.eventitta.region.domain.Region;
import com.eventitta.post.dto.request.CreatePostRequest;
import com.eventitta.post.repository.PostRepository;
import com.eventitta.region.repository.RegionRepository;
import com.eventitta.user.domain.User;
import com.eventitta.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import static com.eventitta.region.exception.RegionErrorCode.NOT_FOUND_REGION_CODE;
import static com.eventitta.user.exception.UserErrorCode.NOT_FOUND_USER_ID;

@Service
@Transactional
@RequiredArgsConstructor
public class PostService {
    private final PostRepository postRepository;
    private final UserRepository userRepository;
    private final RegionRepository regionRepository;

    public Long create(Long userId, CreatePostRequest dto) {
        User user = userRepository.findById(userId)
            .orElseThrow(NOT_FOUND_USER_ID::defaultException);
        Region region = regionRepository.findById(dto.regionCode())
            .orElseThrow(NOT_FOUND_REGION_CODE::defaultException);

        Post post = Post.create(
            user,
            dto.title(),
            dto.content(),
            region
        );
        return postRepository.save(post).getId();
    }
}
