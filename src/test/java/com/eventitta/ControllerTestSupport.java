package com.eventitta;

import com.eventitta.auth.controller.AuthController;
import com.eventitta.auth.jwt.JwtTokenProvider;
import com.eventitta.auth.service.*;
import com.eventitta.comment.controller.CommentController;
import com.eventitta.comment.service.CommentService;
import com.eventitta.common.config.CustomAuthenticationEntryPoint;
import com.eventitta.common.config.SecurityConfig;
import com.eventitta.common.storage.FileStorageService;
import com.eventitta.file.controller.FileUploadController;
import com.eventitta.gamification.service.UserActivityService;
import com.eventitta.post.controller.PostController;
import com.eventitta.post.service.PostService;
import com.eventitta.user.controller.UserController;
import com.eventitta.user.service.UserService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(controllers = {
    AuthController.class,
    PostController.class,
    FileUploadController.class,
    CommentController.class,
    UserController.class
})
@AutoConfigureMockMvc(addFilters = false)
@Import({SecurityConfig.class})
public abstract class ControllerTestSupport {
    @Autowired
    protected MockMvc mockMvc;
    @Autowired
    protected ObjectMapper objectMapper;
    @MockitoBean
    protected AuthService authService;
    @MockitoBean
    protected JwtTokenProvider jwtTokenProvider;
    @MockitoBean
    protected CustomUserDetailsService customUserDetailsService;
    @MockitoBean
    protected LoginService loginService;
    @MockitoBean
    protected TokenService tokenService;
    @MockitoBean
    protected RefreshTokenService refreshService;
    @MockitoBean
    protected CustomAuthenticationEntryPoint customAuthenticationEntryPoint;
    @MockitoBean
    protected PostService postService;
    @MockitoBean
    protected FileStorageService storageService;
    @MockitoBean
    protected CommentService commentService;
    @MockitoBean
    protected UserService userService;
    @MockitoBean
    protected UserActivityService userActivityService;
}
