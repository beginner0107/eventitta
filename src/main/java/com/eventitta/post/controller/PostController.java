package com.eventitta.post.controller;

import com.eventitta.auth.annotation.CurrentUser;
import com.eventitta.common.response.PageResponse;
import com.eventitta.post.domain.Post;
import com.eventitta.post.dto.PostFilter;
import com.eventitta.post.dto.request.CreatePostRequest;
import com.eventitta.post.dto.request.UpdatePostRequest;
import com.eventitta.post.dto.response.CreatePostResponse;
import com.eventitta.post.dto.response.PostDetailResponse;
import com.eventitta.post.dto.response.PostSummaryResponse;
import com.eventitta.post.service.PostService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@RequestMapping("/api/v1/posts")
@RestController
@Tag(name = "지역기반 커뮤니티 API", description = "커뮤니티 게시글 API")
@RequiredArgsConstructor
public class PostController {

    private final PostService postService;

    @Operation(summary = "게시글 생성")
    @ApiResponses({
        @ApiResponse(responseCode = "201", description = "게시글 등록 성공"),
    })
    @PostMapping
    public ResponseEntity<CreatePostResponse> create(
        @CurrentUser Long userId,
        @Valid @RequestBody CreatePostRequest request
    ) {
        CreatePostResponse response = postService.create(userId, request);
        return ResponseEntity
            .created(URI.create("/api/v1/posts/" + response.id()))
            .body(response);
    }

    @Operation(summary = "게시글 목록 조회")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "게시글 목록 조회 성공"),
    })
    @GetMapping
    public ResponseEntity<PageResponse<PostSummaryResponse>> getPosts(
        @Valid PostFilter filter
    ) {
        PageResponse<PostSummaryResponse> result = postService.getPosts(filter);
        return ResponseEntity.ok(result);
    }

    @Operation(summary = "게시글 상세 조회")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "게시글 상세보기 조회 성공"),
    })
    @GetMapping("/{postId}")
    public ResponseEntity<PostDetailResponse> getPost(
        @PathVariable("postId") Long postId
    ) {
        PostDetailResponse result = postService.getPost(postId);
        return ResponseEntity.ok(result);
    }

    @Operation(summary = "게시글 수정")
    @ApiResponses({
        @ApiResponse(responseCode = "204", description = "게시글 수정 성공"),
    })
    @PutMapping(value = "/{postId}")
    public ResponseEntity<Void> update(
        @PathVariable("postId") Long postId,
        @CurrentUser Long userId,
        @RequestBody @Valid UpdatePostRequest request
    ) {
        postService.update(postId, userId, request);
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "게시글 삭제")
    @ApiResponses({
        @ApiResponse(responseCode = "204", description = "게시글 삭제 성공"),
    })
    @DeleteMapping("/{postId}")
    public ResponseEntity<Void> delete(
        @PathVariable("postId") Long postId,
        @CurrentUser Long userId
    ) {
        postService.delete(postId, userId);
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "게시글 추천")
    @ApiResponses({
        @ApiResponse(responseCode = "204", description = "게시글 추천 성공"),
    })
    @PostMapping("/{postId}/like")
    public ResponseEntity<Void> like(@PathVariable("postId") Long postId,
                                     @CurrentUser Long userId) {
        postService.toggleLike(postId, userId);
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "본인이 추천한 게시글 목록 조회")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "본인이 추천한 게시글 목록 조회 성공"),
    })
    @GetMapping("/liked")
    public ResponseEntity<PageResponse<PostSummaryResponse>> likedPosts(
        @CurrentUser Long userId,
        @Valid PostFilter filter
    ) {
        PageResponse<PostSummaryResponse> result = postService.getLikedPosts(userId, filter);
        return ResponseEntity.ok(result);
    }
}
