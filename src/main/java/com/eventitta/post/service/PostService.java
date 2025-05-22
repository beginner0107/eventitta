package com.eventitta.post.service;

import com.eventitta.common.response.PageResponse;
import com.eventitta.post.domain.Post;
import com.eventitta.post.dto.PostFilter;
import com.eventitta.post.dto.request.CreatePostRequest;
import com.eventitta.post.dto.request.PostResponse;
import com.eventitta.post.dto.request.UpdatePostRequest;
import com.eventitta.post.repository.PostRepository;
import com.eventitta.region.domain.Region;
import com.eventitta.region.exception.RegionErrorCode;
import com.eventitta.region.repository.RegionRepository;
import com.eventitta.user.domain.User;
import com.eventitta.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static com.eventitta.post.exception.PostErrorCode.ACCESS_DENIED;
import static com.eventitta.post.exception.PostErrorCode.NOT_FOUND_POST_ID;
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

    public void update(Long postId, Long userId, UpdatePostRequest request) {
        Post post = postRepository.findByIdAndDeletedFalse(postId)
            .orElseThrow(NOT_FOUND_POST_ID::defaultException);

        if (!post.getUser().getId().equals(userId)) {
            throw ACCESS_DENIED.defaultException();
        }
        Region region = regionRepository.findById(request.regionCode())
            .orElseThrow(RegionErrorCode.NOT_FOUND_REGION_CODE::defaultException);

        post.update(request.title(), request.content(), region);
    }

    public void delete(Long postId, Long userId) {
        Post post = postRepository.findByIdAndDeletedFalse(postId)
            .orElseThrow(NOT_FOUND_POST_ID::defaultException);
        if (!post.getUser().getId().equals(userId)) {
            throw ACCESS_DENIED.defaultException();
        }
        post.softDelete();
    }

    public PageResponse<PostResponse> getPosts(PostFilter filter) {
        Pageable pg = PageRequest.of(filter.page(), filter.size());

        Page<Post> posts = postRepository.findAllByFilter(filter, pg);

        List<PostResponse> postResponseList = posts.stream()
            .map(PostResponse::from)
            .toList();

        return new PageResponse<>(
            postResponseList,
            posts.getNumber(),
            posts.getSize(),
            posts.getTotalElements(),
            posts.getTotalPages()
        );
    }

    public PostResponse getPost(Long postId) {
        Post post = postRepository.findByIdAndDeletedFalse(postId)
            .orElseThrow(NOT_FOUND_POST_ID::defaultException);
        return PostResponse.from(post);
    }
}
