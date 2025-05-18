package com.eventitta.post.controller;

import com.eventitta.auth.annotation.CurrentUser;
import com.eventitta.post.dto.request.CreatePostRequest;
import com.eventitta.post.dto.response.CreatePostResponse;
import com.eventitta.post.service.PostService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;

@Slf4j
@RequestMapping("/api/v1/posts")
@RestController
@Tag(name = "지역기반 커뮤니티 API", description = "커뮤니티 게시글 API")
@RequiredArgsConstructor
public class PostController {
    private final PostService postService;


    @Operation(
        summary = "게시글 생성",
        description = "제목, 내용, 지역 코드로 새 게시글을 생성합니다."
    )
    @PostMapping
    public ResponseEntity<CreatePostResponse> create(
        @CurrentUser Long userId,
        @RequestBody @Valid CreatePostRequest request
    ) {
        Long postId = postService.create(userId, request);
        return ResponseEntity
            .created(URI.create("/api/v1/posts/" + postId))
            .body(new CreatePostResponse(postId));
    }
}
