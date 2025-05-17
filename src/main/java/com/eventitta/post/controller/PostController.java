package com.eventitta.post.controller;

import com.eventitta.auth.annotation.CurrentUser;
import com.eventitta.auth.domain.UserPrincipal;
import com.eventitta.post.dto.request.CreatePostRequest;
import com.eventitta.post.dto.response.CreatePostResponse;
import com.eventitta.post.service.PostService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;

@Slf4j
@RequestMapping("/api/v1/posts")
@RestController
@RequiredArgsConstructor
public class PostController {
    private final PostService postService;

    @PostMapping
    public ResponseEntity<CreatePostResponse> create(
        @CurrentUser Long userId,
        @RequestBody @Valid CreatePostRequest request
    ) {
        Long postId = postService.create(userId, request);

        return ResponseEntity
            .created(URI.create("/api/posts/" + postId))
            .body(new CreatePostResponse(postId));
    }
}
