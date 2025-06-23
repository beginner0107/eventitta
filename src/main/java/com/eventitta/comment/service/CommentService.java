package com.eventitta.comment.service;

import com.eventitta.auth.exception.AuthException;
import com.eventitta.comment.domain.Comment;
import com.eventitta.comment.dto.query.CommentFlatDto;
import com.eventitta.comment.dto.response.CommentWithChildrenDto;
import com.eventitta.comment.exception.CommentException;
import com.eventitta.comment.repository.CommentRepository;
import com.eventitta.gamification.service.UserActivityService;
import com.eventitta.post.domain.Post;
import com.eventitta.post.exception.PostException;
import com.eventitta.post.repository.PostRepository;
import com.eventitta.user.domain.User;
import com.eventitta.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static com.eventitta.auth.exception.AuthErrorCode.NOT_FOUND_USER_ID;
import static com.eventitta.comment.exception.CommentErrorCode.NOT_FOUND_COMMENT_ID;
import static com.eventitta.comment.exception.CommentErrorCode.NO_AUTHORITY_TO_MODIFY_COMMENT;
import static com.eventitta.gamification.domain.ActivityType.CREATE_COMMENT;
import static com.eventitta.post.exception.PostErrorCode.NOT_FOUND_POST_ID;

@Service
@RequiredArgsConstructor
@Transactional
public class CommentService {

    private final CommentRepository commentRepository;
    private final PostRepository postRepository;
    private final UserRepository userRepository;
    private final UserActivityService userActivityService;

    public void writeComment(Long postId, Long userId, String content, Long parentCommentId) {
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

        commentRepository.save(comment);

        userActivityService.recordActivity(userId, CREATE_COMMENT);
    }

    @Transactional(readOnly = true)
    public List<CommentWithChildrenDto> getCommentsByPost(Long postId) {
        List<CommentFlatDto> flats = commentRepository.findFlatByPost(postId);

        Map<Long, List<CommentFlatDto>> parentToChildren = flats.stream()
            .filter(dto -> dto.parentId() != null)
            .collect(Collectors.groupingBy(CommentFlatDto::parentId));

        return flats.stream()
            .filter(dto -> dto.parentId() == null)
            .map(parent -> CommentWithChildrenDto.from(parent, parentToChildren.get(parent.id())))
            .toList();
    }

    public void updateComment(Long commentId, Long userId, String content) {
        Comment comment = commentRepository.findById(commentId)
            .orElseThrow(() -> new CommentException(NOT_FOUND_COMMENT_ID));

        if (!comment.getUser().getId().equals(userId)) {
            throw new CommentException(NO_AUTHORITY_TO_MODIFY_COMMENT);
        }

        comment.updateContent(content);
    }

    public void deleteComment(Long commentId, Long userId) {
        Comment comment = commentRepository.findById(commentId)
            .orElseThrow(() -> new CommentException(NOT_FOUND_COMMENT_ID));

        if (!comment.getUser().getId().equals(userId)) {
            throw new CommentException(NO_AUTHORITY_TO_MODIFY_COMMENT);
        }

        comment.softDelete();
    }
}
