package com.eventitta.post.service;

import com.eventitta.auth.exception.AuthErrorCode;
import com.eventitta.auth.exception.AuthException;
import com.eventitta.comment.repository.CommentRepository;
import com.eventitta.common.response.PageResponse;
import com.eventitta.gamification.event.ActivityEventPublisher;
import com.eventitta.post.domain.Post;
import com.eventitta.post.domain.PostImage;
import com.eventitta.post.domain.PostLike;
import com.eventitta.post.dto.PostFilter;
import com.eventitta.post.dto.request.CreatePostRequest;
import com.eventitta.post.dto.request.UpdatePostRequest;
import com.eventitta.post.dto.response.CreatePostResponse;
import com.eventitta.post.dto.response.PostDetailResponse;
import com.eventitta.post.dto.response.PostSummaryResponse;
import com.eventitta.post.event.PostDeleteEventPublisher;
import com.eventitta.post.event.PostDeletedEvent;
import com.eventitta.post.exception.PostException;
import com.eventitta.post.repository.PostLikeRepository;
import com.eventitta.post.repository.PostRepository;
import com.eventitta.region.domain.Region;
import com.eventitta.region.exception.RegionErrorCode;
import com.eventitta.region.repository.RegionRepository;
import com.eventitta.user.domain.User;
import com.eventitta.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import static com.eventitta.gamification.domain.ActivityType.*;
import static com.eventitta.post.exception.PostErrorCode.ACCESS_DENIED;
import static com.eventitta.post.exception.PostErrorCode.NOT_FOUND_POST_ID;
import static com.eventitta.region.exception.RegionErrorCode.NOT_FOUND_REGION_CODE;
import static com.eventitta.user.exception.UserErrorCode.NOT_FOUND_USER_ID;

@Slf4j
@Service
@Transactional
@RequiredArgsConstructor
public class PostService {
    private final PostRepository postRepository;
    private final UserRepository userRepository;
    private final RegionRepository regionRepository;
    private final PostLikeRepository postLikeRepository;
    private final CommentRepository commentRepository;
    private final ActivityEventPublisher activityEventPublisher;
    private final PostDeleteEventPublisher postDeleteEventPublisher;

    public CreatePostResponse create(Long userId, CreatePostRequest dto) {
        User user = userRepository.findById(userId)
            .orElseThrow(NOT_FOUND_USER_ID::defaultException);
        Region region = regionRepository.findById(dto.regionCode())
            .orElseThrow(NOT_FOUND_REGION_CODE::defaultException);

        Post post = Post.create(user, dto.title(), dto.content(), region);
        if (dto.imageUrls() != null) {
            for (int i = 0; i < dto.imageUrls().size(); i++) {
                post.addImage(new PostImage(dto.imageUrls().get(i), i));
            }
        }
        Post savedPost = postRepository.save(post);

        activityEventPublisher.publish(CREATE_POST, userId, savedPost.getId());

        return new CreatePostResponse(savedPost.getId());
    }

    public void update(Long postId, Long userId, UpdatePostRequest dto) {
        Post post = postRepository.findWithUserByIdAndDeletedFalse(postId)
            .orElseThrow(NOT_FOUND_POST_ID::defaultException);
        if (!post.getUser().getId().equals(userId)) {
            throw ACCESS_DENIED.defaultException();
        }

        Region region = regionRepository.findById(dto.regionCode())
            .orElseThrow(RegionErrorCode.NOT_FOUND_REGION_CODE::defaultException);
        post.update(dto.title(), dto.content(), region);

        post.clearImages();
        if (dto.imageUrls() != null) {
            for (int i = 0; i < dto.imageUrls().size(); i++) {
                String url = dto.imageUrls().get(i);
                PostImage img = new PostImage(url, i);
                post.addImage(img);
            }
        }
    }

    public void delete(Long postId, Long userId) {
        Post post = postRepository.findWithUserByIdAndDeletedFalse(postId)
            .orElseThrow(NOT_FOUND_POST_ID::defaultException);
        if (!post.getUser().getId().equals(userId)) {
            throw ACCESS_DENIED.defaultException();
        }

        List<String> imageUrls = post.getImages().stream()
            .map(PostImage::getImageUrl)
            .toList();

        post.clearImages();
        post.softDelete();

        activityEventPublisher.publishRevoke(DELETE_POST, userId, postId);
        postDeleteEventPublisher.publish(new PostDeletedEvent(imageUrls));
    }

    @Transactional(readOnly = true)
    public PageResponse<PostSummaryResponse> getPosts(PostFilter filter) {
        Pageable pg = PageRequest.of(filter.page(), filter.size());
        Page<PostSummaryResponse> page = postRepository.findSummaries(filter, pg);

        return new PageResponse<>(
            page.getContent(),
            page.getNumber(),
            page.getSize(),
            page.getTotalElements(),
            page.getTotalPages()
        );
    }

    @Transactional(readOnly = true)
    public PostDetailResponse getPost(Long postId) {
        Post post = postRepository.findDetailByIdAndDeletedFalse(postId)
            .orElseThrow(NOT_FOUND_POST_ID::defaultException);

        int commentCount = commentRepository.countByPostIdAndDeletedFalse(postId);

        return PostDetailResponse.from(post, commentCount);
    }

    @Transactional
    public void toggleLike(Long postId, Long userId) {
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new AuthException(AuthErrorCode.NOT_FOUND_USER_ID));
        Post post = postRepository.findById(postId)
            .orElseThrow(() -> new PostException(NOT_FOUND_POST_ID));

        Optional<PostLike> existing = postLikeRepository.findByPostIdAndUserId(postId, userId);
        if (existing.isPresent()) {
            postLikeRepository.delete(existing.get());
            post.decrementLikeCount();
            activityEventPublisher.publishRevoke(LIKE_POST_CANCEL, userId, postId);
        } else {
            PostLike like = new PostLike(post, user);
            postLikeRepository.save(like);
            post.incrementLikeCount();
            activityEventPublisher.publish(LIKE_POST, userId, postId);
        }
    }

    @Transactional(readOnly = true)
    public PageResponse<PostSummaryResponse> getLikedPosts(Long userId, PostFilter filter) {
        Pageable pg = PageRequest.of(filter.page(), filter.size());
        Page<PostSummaryResponse> page = postLikeRepository.findLikedSummaries(userId, filter, pg);
        return PageResponse.of(page);
    }
}
