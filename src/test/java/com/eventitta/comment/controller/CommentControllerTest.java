package com.eventitta.comment.controller;

import com.eventitta.ControllerTestSupport;
import com.eventitta.WithMockCustomUser;
import com.eventitta.comment.dto.request.CommentRequestDto;
import com.eventitta.comment.dto.request.CommentUpdateRequestDto;
import com.eventitta.comment.dto.response.CommentWithChildrenDto;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import java.util.List;

import static com.eventitta.common.constants.ValidationMessage.COMMENT_CONTENT;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class CommentControllerTest extends ControllerTestSupport {

    private final Long postId = 1L;
    private final Long commentId = 10L;
    private final Long userId = 42L;

    @Test
    @WithMockCustomUser
    @DisplayName("댓글을 정상적으로 등록할 수 있다")
    void createComment_withValidInput_returnsCreated() throws Exception {
        // given
        CommentRequestDto request = new CommentRequestDto("댓글 내용입니다.", null);
        given(commentService.writeComment(postId, userId, request.content(), request.parentCommentId()))
            .willReturn(List.of("첫 댓글 작성 성공"));

        // when & then
        mockMvc.perform(post("/api/v1/posts/{postId}/comments", postId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isCreated());

        verify(commentService).writeComment(postId, userId, request.content(), request.parentCommentId());
    }

    @Test
    @WithMockCustomUser
    @DisplayName("댓글 내용이 비어 있으면 400 에러가 발생한다")
    void createComment_withBlankContent_returnsBadRequest() throws Exception {
        // given
        CommentRequestDto request = new CommentRequestDto(" ", null);

        // when & then
        mockMvc.perform(post("/api/v1/posts/{postId}/comments", postId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.message").value("content: " + COMMENT_CONTENT));
    }

    @Test
    @DisplayName("댓글 목록을 조회하면 200과 함께 댓글 리스트를 반환한다")
    void getComments_withValidPostId_returnsList() throws Exception {
        // given
        List<CommentWithChildrenDto> comments = List.of();
        given(commentService.getCommentsByPost(postId)).willReturn(comments);

        // when & then
        mockMvc.perform(get("/api/v1/posts/{postId}/comments", postId))
            .andExpect(status().isOk())
            .andExpect(content().json("[]"));

        verify(commentService).getCommentsByPost(postId);
    }

    @Test
    @WithMockCustomUser
    @DisplayName("댓글을 정상적으로 수정할 수 있다")
    void updateComment_withValidInput_returnsNoContent() throws Exception {
        // given
        CommentUpdateRequestDto request = new CommentUpdateRequestDto("수정된 댓글입니다.");
        doNothing().when(commentService).updateComment(commentId, userId, request.content());

        // when & then
        mockMvc.perform(put("/api/v1/posts/{postId}/comments/{commentId}", postId, commentId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isNoContent());

        verify(commentService).updateComment(commentId, userId, request.content());
    }

    @Test
    @WithMockCustomUser
    @DisplayName("댓글 수정 시 내용이 비어 있으면 400 에러가 발생한다")
    void updateComment_withBlankContent_returnsBadRequest() throws Exception {
        // given
        CommentUpdateRequestDto request = new CommentUpdateRequestDto(" ");

        // when & then
        mockMvc.perform(put("/api/v1/posts/{postId}/comments/{commentId}", postId, commentId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.message").value("content: " + COMMENT_CONTENT));
    }

    @Test
    @WithMockCustomUser
    @DisplayName("댓글을 정상적으로 삭제할 수 있다")
    void deleteComment_withValidInput_returnsNoContent() throws Exception {
        // given
        doNothing().when(commentService).deleteComment(commentId, userId);

        // when & then
        mockMvc.perform(delete("/api/v1/posts/{postId}/comments/{commentId}", postId, commentId))
            .andExpect(status().isNoContent());

        verify(commentService).deleteComment(commentId, userId);
    }
}
