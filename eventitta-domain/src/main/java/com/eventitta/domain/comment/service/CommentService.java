package com.eventitta.domain.comment.service;

import com.eventitta.domain.comment.domain.Comment;
import com.eventitta.domain.comment.dto.projection.CommentFlatProjection;
import com.eventitta.domain.comment.dto.response.CommentWithChildrenResponse;
import com.eventitta.domain.comment.repository.CommentRepository;
import com.eventitta.domain.gamification.api.internal.facade.GamificationInternalFacade;
import com.eventitta.domain.post.api.internal.facade.PostInternalFacade;
import com.eventitta.domain.user.api.internal.facade.UserInternalFacade;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static com.eventitta.domain.comment.exception.CommentErrorCode.NOT_FOUND_COMMENT_ID;
import static com.eventitta.domain.comment.exception.CommentErrorCode.NO_AUTHORITY_TO_MODIFY_COMMENT;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class CommentService {

    private final CommentRepository commentRepository;
    private final PostInternalFacade postInternalFacade;
    private final UserInternalFacade userInternalFacade;
    private final GamificationInternalFacade gamificationFacade;

    public void writeComment(Long postId, Long userId, String content, Long parentCommentId) {
        log.info("[댓글 생성 시작] userId={}, postId={}, parentCommentId={}",
            userId, postId, parentCommentId);

        postInternalFacade.ensureActivePost(postId);
        userInternalFacade.ensureActiveUser(userId);

        Comment parent = null;
        if (parentCommentId != null) {
            parent = commentRepository.findById(parentCommentId)
                .orElseThrow(NOT_FOUND_COMMENT_ID::defaultException);
        }

        Comment comment = Comment.builder()
            .postId(postId)
            .userId(userId)
            .content(content)
            .parent(parent)
            .build();

        Comment savedComment = commentRepository.save(comment);

        gamificationFacade.onCommentCreated(userId, savedComment.getId());

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
            .orElseThrow(NOT_FOUND_COMMENT_ID::defaultException);

        if (!comment.getUserId().equals(userId)) {
            log.warn("[댓글 수정 권한 없음] userId={}, commentId={}, ownerId={}",
                userId, commentId, comment.getUserId());
            throw NO_AUTHORITY_TO_MODIFY_COMMENT.defaultException();
        }

        comment.updateContent(content);

        log.info("[댓글 수정 완료] userId={}, commentId={}", userId, commentId);
    }

    public void deleteComment(Long commentId, Long userId) {
        log.info("[댓글 삭제 시작] userId={}, commentId={}", userId, commentId);

        Comment comment = commentRepository.findById(commentId)
            .orElseThrow(NOT_FOUND_COMMENT_ID::defaultException);

        if (!comment.getUserId().equals(userId)) {
            log.warn("[댓글 삭제 권한 없음] userId={}, commentId={}, ownerId={}",
                userId, commentId, comment.getUserId());
            throw NO_AUTHORITY_TO_MODIFY_COMMENT.defaultException();
        }
        comment.softDelete();
        gamificationFacade.onCommentDeleted(userId, commentId);

        log.info("[댓글 삭제 완료] userId={}, commentId={}", userId, commentId);
    }
}
