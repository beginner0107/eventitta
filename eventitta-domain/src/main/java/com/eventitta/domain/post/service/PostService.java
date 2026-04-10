package com.eventitta.domain.post.service;

import com.eventitta.domain.comment.api.internal.facade.CommentQueryFacade;
import com.eventitta.domain.common.response.PageResponse;
import com.eventitta.domain.gamification.api.internal.facade.GamificationInternalFacade;
import com.eventitta.domain.media.api.internal.facade.MediaAssetInternalFacade;
import com.eventitta.domain.media.api.internal.view.MediaAttachmentView;
import com.eventitta.domain.media.api.internal.view.MediaDisplayView;
import com.eventitta.domain.media.domain.MediaCategory;
import com.eventitta.domain.post.domain.Post;
import com.eventitta.domain.post.domain.PostImage;
import com.eventitta.domain.post.domain.PostLike;
import com.eventitta.domain.post.api.internal.facade.PostInternalFacade;
import com.eventitta.domain.post.dto.PostFilter;
import com.eventitta.domain.post.dto.request.CreatePostRequest;
import com.eventitta.domain.post.dto.request.UpdatePostRequest;
import com.eventitta.domain.post.dto.response.CreatePostResponse;
import com.eventitta.domain.post.dto.response.PostDetailResponse;
import com.eventitta.domain.post.dto.response.PostImageResponse;
import com.eventitta.domain.post.dto.response.PostLikeStateResponse;
import com.eventitta.domain.post.dto.response.PostSummaryResponse;
import com.eventitta.domain.post.repository.PostLikeRepository;
import com.eventitta.domain.post.repository.PostRepository;
import com.eventitta.domain.region.api.internal.facade.RegionInternalFacade;
import com.eventitta.domain.user.api.internal.facade.UserInternalFacade;
import com.eventitta.domain.user.api.internal.view.UserProfileView;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.Comparator;
import java.util.Map;
import java.util.stream.Collectors;

import static com.eventitta.domain.post.exception.PostErrorCode.ACCESS_DENIED;
import static com.eventitta.domain.post.exception.PostErrorCode.NOT_FOUND_POST_ID;

@Slf4j
@Service
@Transactional
@RequiredArgsConstructor
public class PostService {
    private final PostRepository postRepository;
    private final PostLikeRepository postLikeRepository;
    private final CommentQueryFacade commentQueryFacade;
    private final GamificationInternalFacade gamificationFacade;
    private final UserInternalFacade userInternalFacade;
    private final RegionInternalFacade regionInternalFacade;
    private final MediaAssetInternalFacade mediaAssetInternalFacade;

    public CreatePostResponse create(Long userId, CreatePostRequest dto) {
        log.info("[게시글 생성 시작] userId={}, title={}", userId, dto.title());

        userInternalFacade.ensureActiveUser(userId);
        regionInternalFacade.ensureExists(dto.regionCode());

        Post post = Post.create(userId, dto.title(), dto.content(), dto.regionCode());
        List<MediaAttachmentView> attachments = mediaAssetInternalFacade.resolveAndAttachAssets(
            userId,
            MediaCategory.POST_IMAGE,
            dto.imageMediaIds()
        );
        if (dto.imageMediaIds() != null) {
            for (int i = 0; i < attachments.size(); i++) {
                MediaAttachmentView attachment = attachments.get(i);
                post.addImage(new PostImage(attachment.mediaId(), attachment.publicUrl(), i));
            }
        }
        Post savedPost = postRepository.save(post);

        gamificationFacade.onPostCreated(userId, savedPost.getId());

        log.info("[게시글 생성 완료] userId={}, postId={}, imageCount={}",
            userId, savedPost.getId(), dto.imageMediaIds() != null ? dto.imageMediaIds().size() : 0);

        return new CreatePostResponse(savedPost.getId());
    }

    public void update(Long postId, Long userId, UpdatePostRequest dto) {
        log.info("[게시글 수정 시작] userId={}, postId={}", userId, postId);

        userInternalFacade.ensureActiveUser(userId);

        Post post = postRepository.findWithImagesByIdAndDeletedFalse(postId)
            .orElseThrow(NOT_FOUND_POST_ID::defaultException);
        if (!post.isAuthor(userId)) {
            log.warn("[게시글 수정 권한 없음] userId={}, postId={}, ownerId={}",
                userId, postId, post.getAuthorUserId());
            throw ACCESS_DENIED.defaultException();
        }

        regionInternalFacade.ensureExists(dto.regionCode());
        post.update(dto.title(), dto.content(), dto.regionCode());

        List<Long> currentMediaIds = post.getImages().stream()
            .map(PostImage::getMediaAssetId)
            .toList();
        List<MediaAttachmentView> attachments = mediaAssetInternalFacade.resolveAndAttachAssets(
            userId,
            MediaCategory.POST_IMAGE,
            dto.imageMediaIds()
        );
        Set<Long> requestedMediaIds = attachments.stream()
            .map(MediaAttachmentView::mediaId)
            .collect(Collectors.toSet());

        post.clearImages();
        if (dto.imageMediaIds() != null) {
            for (int i = 0; i < attachments.size(); i++) {
                MediaAttachmentView attachment = attachments.get(i);
                post.addImage(new PostImage(attachment.mediaId(), attachment.publicUrl(), i));
            }
        }
        postRepository.flush();
        List<Long> removedMediaIds = currentMediaIds.stream()
            .filter(mediaId -> !requestedMediaIds.contains(mediaId))
            .toList();
        mediaAssetInternalFacade.releaseAssetsIfUnreferenced(removedMediaIds);

        log.info("[게시글 수정 완료] userId={}, postId={}, imageCount={}",
            userId, postId, dto.imageMediaIds() != null ? dto.imageMediaIds().size() : 0);
    }

    public void delete(Long postId, Long userId) {
        log.info("[게시글 삭제 시작] userId={}, postId={}", userId, postId);

        userInternalFacade.ensureActiveUser(userId);

        Post post = postRepository.findWithImagesByIdAndDeletedFalse(postId)
            .orElseThrow(NOT_FOUND_POST_ID::defaultException);
        if (!post.isAuthor(userId)) {
            log.warn("[게시글 삭제 권한 없음] userId={}, postId={}, ownerId={}",
                userId, postId, post.getAuthorUserId());
            throw ACCESS_DENIED.defaultException();
        }

        List<Long> imageMediaIds = post.getImages().stream()
            .map(PostImage::getMediaAssetId)
            .toList();

        post.clearImages();
        post.softDelete();
        postRepository.flush();
        mediaAssetInternalFacade.releaseAssetsIfUnreferenced(imageMediaIds);

        gamificationFacade.onPostDeleted(userId, postId);

        log.info("[게시글 삭제 완료] userId={}, postId={}, imageCount={}",
            userId, postId, imageMediaIds.size());
    }

    @Transactional(readOnly = true)
    public PageResponse<PostSummaryResponse> getPosts(Long currentUserId, PostFilter filter) {
        Pageable pg = PageRequest.of(filter.page(), filter.size());
        Page<PostSummaryResponse> page = postRepository.findSummaries(filter, pg, currentUserId);

        return new PageResponse<>(
            page.getContent(),
            page.getNumber(),
            page.getSize(),
            page.getTotalElements(),
            page.getTotalPages()
        );
    }

    @Transactional(readOnly = true)
    public PostDetailResponse getPost(Long postId, Long currentUserId) {
        Post post = postRepository.findWithImagesByIdAndDeletedFalse(postId)
            .orElseThrow(NOT_FOUND_POST_ID::defaultException);

        int likeCount = getLikeCount(postId);
        boolean likedByMe = currentUserId != null && postLikeRepository.existsByPostIdAndUserId(postId, currentUserId);
        int commentCount = commentQueryFacade.countActiveCommentsByPostId(postId);
        List<Long> mediaIds = post.getImages().stream()
            .map(PostImage::getMediaAssetId)
            .toList();
        Map<Long, MediaDisplayView> displayInfoByAssetId = mediaAssetInternalFacade.resolveDisplayViews(mediaIds);
        List<PostImageResponse> images = post.getImages().stream()
            .sorted(Comparator.comparingInt(PostImage::getSortOrder))
            .map(image -> {
                MediaDisplayView displayInfo = displayInfoByAssetId.getOrDefault(
                    image.getMediaAssetId(),
                    new MediaDisplayView(image.getImageUrl(), image.getImageUrl(), null, null, com.eventitta.domain.media.domain.MediaProcessingStatus.READY)
                );
                return new PostImageResponse(
                    image.getId(),
                    displayInfo.imageUrl(),
                    displayInfo.thumbnailUrl(),
                    displayInfo.width(),
                    displayInfo.height(),
                    displayInfo.processingStatus(),
                    image.getSortOrder()
                );
            })
            .toList();
        UserProfileView authorProfile = userInternalFacade.findUserProfile(post.getAuthorUserId()).orElse(null);
        String authorNickname = authorProfile != null ? authorProfile.nickname() : "알 수 없음";
        String authorProfileUrl = null;
        if (authorProfile != null) {
            authorProfileUrl = authorProfile.profilePictureMediaId() != null
                ? mediaAssetInternalFacade.resolveDisplayUrl(authorProfile.profilePictureMediaId())
                : authorProfile.profilePictureUrl();
        }

        return PostDetailResponse.from(post, authorNickname, authorProfileUrl, likeCount, likedByMe, commentCount, images);
    }

    @Transactional
    public PostLikeStateResponse likePost(Long postId, Long userId) {
        userInternalFacade.ensureActiveUser(userId);
        Post post = postRepository.findByIdAndDeletedFalse(postId)
            .orElseThrow(NOT_FOUND_POST_ID::defaultException);

        if (!postLikeRepository.existsByPostIdAndUserId(postId, userId)) {
            PostLike like = new PostLike(post, userId);
            postLikeRepository.save(like);
            log.info("[게시글 좋아요] userId={}, postId={}", userId, postId);
        }

        return PostLikeStateResponse.liked(postId, getLikeCount(postId));
    }

    @Transactional
    public PostLikeStateResponse unlikePost(Long postId, Long userId) {
        userInternalFacade.ensureActiveUser(userId);
        postRepository.findByIdAndDeletedFalse(postId)
            .orElseThrow(NOT_FOUND_POST_ID::defaultException);

        Optional<PostLike> existing = postLikeRepository.findByPostIdAndUserId(postId, userId);
        if (existing.isPresent()) {
            postLikeRepository.delete(existing.get());
            log.info("[게시글 좋아요 취소] userId={}, postId={}", userId, postId);
        }

        return PostLikeStateResponse.unliked(postId, getLikeCount(postId));
    }

    @Transactional(readOnly = true)
    public PageResponse<PostSummaryResponse> getLikedPosts(Long userId, PostFilter filter) {
        Pageable pg = PageRequest.of(filter.page(), filter.size());
        Page<PostSummaryResponse> page = postLikeRepository.findLikedSummaries(userId, filter, pg);
        return PageResponse.of(page);
    }

    private int getLikeCount(Long postId) {
        return Math.toIntExact(postLikeRepository.countByPostId(postId));
    }
}
