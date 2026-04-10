package com.eventitta.domain.comment.service;

import com.eventitta.domain.comment.domain.Comment;
import com.eventitta.domain.comment.repository.CommentRepository;
import com.eventitta.domain.gamification.api.internal.facade.GamificationInternalFacade;
import com.eventitta.domain.post.api.internal.facade.PostInternalFacade;
import com.eventitta.domain.user.api.internal.facade.UserInternalFacade;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CommentServiceTest {

    @Mock
    private CommentRepository commentRepository;

    @Mock
    private PostInternalFacade postInternalFacade;

    @Mock
    private UserInternalFacade userInternalFacade;

    @Mock
    private GamificationInternalFacade gamificationFacade;

    @InjectMocks
    private CommentService commentService;

    @Test
    @DisplayName("댓글 생성 시 postId와 userId 기반으로 저장하고 게임화 이벤트를 발행한다")
    void writeComment_savesScalarIdsAndPublishesGamificationEvent() {
        when(commentRepository.save(any(Comment.class))).thenAnswer(invocation -> {
            Comment comment = invocation.getArgument(0);
            ReflectionTestUtils.setField(comment, "id", 100L);
            return comment;
        });

        commentService.writeComment(11L, 22L, "댓글 내용", null);

        verify(postInternalFacade).ensureActivePost(11L);
        verify(userInternalFacade).ensureActiveUser(22L);
        verify(commentRepository).save(any(Comment.class));
        verify(gamificationFacade).onCommentCreated(22L, 100L);
    }

    @Test
    @DisplayName("작성자 본인이 댓글을 삭제하면 soft delete 후 게임화 이벤트를 발행한다")
    void deleteComment_softDeletesOwnedComment() {
        Comment comment = Comment.builder()
            .postId(11L)
            .userId(22L)
            .content("댓글 내용")
            .build();
        ReflectionTestUtils.setField(comment, "id", 100L);
        when(commentRepository.findById(100L)).thenReturn(Optional.of(comment));

        commentService.deleteComment(100L, 22L);

        assertThat(comment.isDeleted()).isTrue();
        verify(gamificationFacade).onCommentDeleted(22L, 100L);
    }
}
