package com.eventitta.comment.service;

import com.eventitta.auth.exception.AuthException;
import com.eventitta.comment.domain.Comment;
import com.eventitta.comment.dto.projection.CommentFlatProjection;
import com.eventitta.comment.dto.response.CommentWithChildrenResponse;
import com.eventitta.comment.exception.CommentException;
import com.eventitta.comment.repository.CommentRepository;
import com.eventitta.gamification.domain.ActivityType;
import com.eventitta.gamification.event.ActivityEventPublisher;
import com.eventitta.post.domain.Post;
import com.eventitta.post.exception.PostException;
import com.eventitta.post.repository.PostRepository;
import com.eventitta.user.domain.User;
import com.eventitta.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static com.eventitta.auth.exception.AuthErrorCode.NOT_FOUND_USER_ID;
import static com.eventitta.comment.exception.CommentErrorCode.NOT_FOUND_COMMENT_ID;
import static com.eventitta.comment.exception.CommentErrorCode.NO_AUTHORITY_TO_MODIFY_COMMENT;
import static com.eventitta.gamification.domain.ActivityType.*;
import static com.eventitta.post.exception.PostErrorCode.NOT_FOUND_POST_ID;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class CommentService {

    private final CommentRepository commentRepository;
    private final PostRepository postRepository;
    private final UserRepository userRepository;
    private final ActivityEventPublisher activityEventPublisher;

    public void writeComment(Long postId, Long userId, String content, Long parentCommentId) {
        log.info("[댓글 생성 시작] userId={}, postId={}, parentCommentId={}",
            userId, postId, parentCommentId);

        Post post = postRepository.findById(postId)
            .orElseThrow(() -> new PostException(NOT_FOUND_POST_ID));
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new AuthException(NOT_FOUND_USER_ID));

        Comment parent = null;
        if (parentCommentId != null) {
            parent = commentRepository.findById(parentCommentId)
                .orElseThrow(() -> new CommentException(NOT_FOUND_COMMENT_ID));
        }

        Comment comment = Comment.builder()
            .post(post)
            .user(user)
            .content(content)
            .parent(parent)
            .build();

        Comment savedComment = commentRepository.save(comment);

        activityEventPublisher.publish(CREATE_COMMENT, userId, savedComment.getId());

        log.info("[댓글 생성 완료] userId={}, postId={}, commentId={}, isReply={}",
            userId, postId, savedComment.getId(), parentCommentId != null);
    }

    @Transactional(readOnly = true)
    public List<CommentWithChildrenResponse> getCommentsByPost(Long postId) {
        List<CommentFlatProjection> flats = commentRepository.findFlatByPost(postId);

        Map<Long, List<CommentFlatProjection>> parentToChildren = flats.stream()
            .filter(dto -> dto.parentId() != null)
            .collect(Collectors.groupingBy(CommentFlatProjection::parentId));

        return flats.stream()
            .filter(dto -> dto.parentId() == null)
            .map(parent -> CommentWithChildrenResponse.from(parent, parentToChildren.get(parent.id())))
            .toList();
    }

    public void updateComment(Long commentId, Long userId, String content) {
        log.info("[댓글 수정 시작] userId={}, commentId={}", userId, commentId);

        Comment comment = commentRepository.findById(commentId)
            .orElseThrow(() -> new CommentException(NOT_FOUND_COMMENT_ID));

        if (!comment.getUser().getId().equals(userId)) {
            log.warn("[댓글 수정 권한 없음] userId={}, commentId={}, ownerId={}",
                userId, commentId, comment.getUser().getId());
            throw new CommentException(NO_AUTHORITY_TO_MODIFY_COMMENT);
        }

        comment.updateContent(content);

        log.info("[댓글 수정 완료] userId={}, commentId={}", userId, commentId);
    }

    public void deleteComment(Long commentId, Long userId) {
        log.info("[댓글 삭제 시작] userId={}, commentId={}", userId, commentId);

        Comment comment = commentRepository.findById(commentId)
            .orElseThrow(() -> new CommentException(NOT_FOUND_COMMENT_ID));

        if (!comment.getUser().getId().equals(userId)) {
            log.warn("[댓글 삭제 권한 없음] userId={}, commentId={}, ownerId={}",
                userId, commentId, comment.getUser().getId());
            throw new CommentException(NO_AUTHORITY_TO_MODIFY_COMMENT);
        }
        comment.softDelete();
        activityEventPublisher.publishRevoke(DELETE_COMMENT, userId, commentId);

        log.info("[댓글 삭제 완료] userId={}, commentId={}", userId, commentId);
    }
}
