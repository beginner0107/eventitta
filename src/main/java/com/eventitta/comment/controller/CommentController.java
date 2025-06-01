package com.eventitta.comment.controller;

import com.eventitta.auth.annotation.CurrentUser;
import com.eventitta.comment.dto.request.CommentRequestDto;
import com.eventitta.comment.dto.request.CommentUpdateRequestDto;
import com.eventitta.comment.dto.response.CommentWithChildrenDto;
import com.eventitta.comment.service.CommentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/posts/{postId}/comments")
@Tag(name = "댓글 API", description = "게시글 댓글 관련 API")
public class CommentController {

    private final CommentService commentService;

    @Operation(summary = "댓글 작성")
    @PostMapping
    public ResponseEntity<Void> writeComment(
        @PathVariable("postId") Long postId,
        @CurrentUser Long userId,
        @RequestBody @Valid CommentRequestDto request
    ) {
        commentService.writeComment(postId, userId, request.content(), request.parentCommentId());
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    @Operation(summary = "댓글 목록 조회")
    @GetMapping
    public ResponseEntity<List<CommentWithChildrenDto>> getComments(
        @PathVariable("postId") Long postId
    ) {
        List<CommentWithChildrenDto> comments = commentService.getCommentsByPost(postId);
        return ResponseEntity.ok(comments);
    }

    @Operation(summary = "댓글 수정")
    @PutMapping("/{commentId}")
    public ResponseEntity<Void> updateComment(
        @PathVariable("postId") Long postId,
        @PathVariable("commentId") Long commentId,
        @CurrentUser Long userId,
        @RequestBody @Valid CommentUpdateRequestDto request
    ) {
        commentService.updateComment(commentId, userId, request.content());
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "댓글 삭제")
    @DeleteMapping("/{commentId}")
    public ResponseEntity<Void> deleteComment(
        @PathVariable("postId") Long postId,
        @PathVariable("commentId") Long commentId,
        @CurrentUser Long userId
    ) {
        commentService.deleteComment(commentId, userId);
        return ResponseEntity.noContent().build();
    }
}
