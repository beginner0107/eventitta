package com.eventitta.comment.service;

import com.eventitta.comment.domain.Comment;
import com.eventitta.comment.exception.CommentException;
import com.eventitta.comment.repository.CommentRepository;
import com.eventitta.gamification.activitylog.ActivityEventPublisher;
import com.eventitta.gamification.constant.ActivityCodes;
import com.eventitta.post.domain.Post;
import com.eventitta.post.exception.PostException;
import com.eventitta.post.repository.PostRepository;
import com.eventitta.user.domain.User;
import com.eventitta.user.repository.UserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static com.eventitta.comment.exception.CommentErrorCode.NOT_FOUND_COMMENT_ID;
import static com.eventitta.comment.exception.CommentErrorCode.NO_AUTHORITY_TO_MODIFY_COMMENT;
import static com.eventitta.post.exception.PostErrorCode.NOT_FOUND_POST_ID;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class CommentServiceTest {

    @InjectMocks
    private CommentService commentService;

    @Mock
    private CommentRepository commentRepository;

    @Mock
    private PostRepository postRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private ActivityEventPublisher activityEventPublisher;

    private final Long postId = 1L;
    private final Long commentId = 100L;
    private final Long userId = 42L;

    @Test
    @DisplayName("댓글 작성자 본인인 경우 댓글을 작성할 수 있다")
    void givenPostAndUser_whenWriteComment_thenSaveComment() {
        // given
        Post post = Post.builder().id(postId).build();
        User user = User.builder().id(userId).build();

        given(postRepository.findById(postId)).willReturn(Optional.of(post));
        given(userRepository.findById(userId)).willReturn(Optional.of(user));

        long fakeCommentId = 123L;
        given(commentRepository.save(any(Comment.class)))
            .willReturn(Comment.builder()
                .id(fakeCommentId)
                .post(post)
                .user(user)
                .content("내용")
                .build());

        // when
        commentService.writeComment(postId, userId, "내용", null);

        // then
        verify(commentRepository).save(any(Comment.class));
        verify(activityEventPublisher).publish(ActivityCodes.CREATE_COMMENT, userId, fakeCommentId);
    }

    @Test
    @DisplayName("게시글이 존재하지 않으면 댓글 작성에 실패한다")
    void givenPostNotFound_whenWriteComment_thenThrowPostException() {
        // given
        given(postRepository.findById(postId)).willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() ->
            commentService.writeComment(postId, userId, "내용", null)
        ).isInstanceOf(PostException.class)
            .hasMessageContaining(NOT_FOUND_POST_ID.defaultMessage());
    }

    @Test
    @DisplayName("댓글 작성자 본인인 경우 댓글 내용을 수정할 수 있다")
    void givenExistingCommentOwner_whenUpdateComment_thenContentIsUpdated() {
        // given
        Comment comment = Comment.builder()
            .id(commentId)
            .user(User.builder().id(userId).build())
            .content("이전 내용")
            .build();

        given(commentRepository.findById(commentId)).willReturn(Optional.of(comment));

        // when
        commentService.updateComment(commentId, userId, "수정된 내용");

        // then
        assertThat(comment.getContent()).isEqualTo("수정된 내용");
    }

    @Test
    @DisplayName("댓글 작성자가 아니면 댓글 수정에 실패한다")
    void givenNotCommentOwner_whenUpdateComment_thenThrowCommentException() {
        // given
        Comment comment = Comment.builder()
            .id(commentId)
            .user(User.builder().id(999L).build())
            .content("기존 내용")
            .build();

        given(commentRepository.findById(commentId)).willReturn(Optional.of(comment));

        // when & then
        assertThatThrownBy(() ->
            commentService.updateComment(commentId, userId, "수정 내용")
        ).isInstanceOf(CommentException.class)
            .hasMessageContaining(NO_AUTHORITY_TO_MODIFY_COMMENT.defaultMessage());
    }

    @Test
    @DisplayName("댓글 작성자 본인인 경우 댓글을 삭제할 수 있다")
    void givenExistingCommentOwner_whenDeleteComment_thenMarkDeleted() {
        // given
        Comment comment = Comment.builder()
            .id(commentId)
            .user(User.builder().id(userId).build())
            .deleted(false)
            .build();

        given(commentRepository.findById(commentId)).willReturn(Optional.of(comment));

        // when
        commentService.deleteComment(commentId, userId);

        // then
        assertThat(comment.isDeleted()).isTrue();
    }

    @Test
    @DisplayName("댓글 작성자가 아니면 댓글 수정에 실패한다")
    void givenNotCommentOwner_whenDeleteComment_thenThrowCommentException() {
        // given
        Comment comment = Comment.builder()
            .id(commentId)
            .user(User.builder().id(999L).build())
            .deleted(false)
            .build();

        given(commentRepository.findById(commentId)).willReturn(Optional.of(comment));

        // when & then
        assertThatThrownBy(() ->
            commentService.deleteComment(commentId, userId)
        ).isInstanceOf(CommentException.class)
            .hasMessageContaining(NO_AUTHORITY_TO_MODIFY_COMMENT.defaultMessage());
    }

    @Test
    @DisplayName("부모 댓글 ID가 잘못된 경우 CommentException이 발생한다")
    void givenInvalidParentId_whenWriteComment_thenThrowCommentException() {
        // given
        Post post = Post.builder().id(postId).build();
        User user = User.builder().id(userId).build();
        Long invalidParentId = 999L;

        given(postRepository.findById(postId)).willReturn(Optional.of(post));
        given(userRepository.findById(userId)).willReturn(Optional.of(user));
        given(commentRepository.findById(invalidParentId)).willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() ->
            commentService.writeComment(postId, userId, "내용", invalidParentId)
        ).isInstanceOf(CommentException.class)
            .hasMessageContaining(NOT_FOUND_COMMENT_ID.defaultMessage());
    }

    @Test
    @DisplayName("대상 댓글이 없으면 댓글 수정에 실패한다")
    void givenCommentNotFound_whenUpdateComment_thenThrowCommentException() {
        // given
        given(commentRepository.findById(commentId)).willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() ->
            commentService.updateComment(commentId, userId, "수정 내용")
        ).isInstanceOf(CommentException.class)
            .hasMessageContaining(NOT_FOUND_COMMENT_ID.defaultMessage());
    }

    @Test
    @DisplayName("대상 댓글이 없으면 댓글 삭제에 실패한다")
    void givenCommentNotFound_whenDeleteComment_thenThrowCommentException() {
        // given
        given(commentRepository.findById(commentId)).willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() ->
            commentService.deleteComment(commentId, userId)
        ).isInstanceOf(CommentException.class)
            .hasMessageContaining(NOT_FOUND_COMMENT_ID.defaultMessage());
    }

    @Test
    @DisplayName("댓글 삭제 시 활동 취소 이벤트가 발행된다")
    void givenCommentOwner_whenDeleteComment_thenPublishRevokeEvent() {
        // given
        Comment comment = createComment(commentId, userId, false);
        given(commentRepository.findById(commentId)).willReturn(Optional.of(comment));

        // when
        commentService.deleteComment(commentId, userId);

        // then
        verify(activityEventPublisher).publishRevoke(ActivityCodes.CREATE_COMMENT, userId, commentId);
    }

    private Comment createComment(Long id, Long authorId, boolean deleted) {
        return Comment.builder()
            .id(id)
            .user(User.builder().id(authorId).build())
            .content("내용")
            .deleted(deleted)
            .build();
    }
}
