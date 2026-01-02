package com.eventitta;

import com.eventitta.auth.controller.AuthController;
import com.eventitta.auth.jwt.JwtAuthenticationEntryPoint;
import com.eventitta.auth.jwt.JwtTokenProvider;
import com.eventitta.auth.config.SecurityConfig;
import com.eventitta.auth.jwt.service.CustomUserDetailsService;
import com.eventitta.auth.jwt.service.UserInfoService;
import com.eventitta.auth.service.AuthService;
import com.eventitta.auth.service.LoginService;
import com.eventitta.auth.service.RefreshTokenService;
import com.eventitta.auth.service.TokenService;
import com.eventitta.comment.controller.CommentController;
import com.eventitta.comment.service.CommentService;
import com.eventitta.notification.resolver.AlertLevelResolver;
import com.eventitta.notification.service.DiscordNotificationService;
import com.eventitta.file.controller.FileUploadController;
import com.eventitta.file.service.FileStorageService;
import com.eventitta.file.service.FileValidationService;
import com.eventitta.gamification.service.UserActivityService;
import com.eventitta.post.controller.PostController;
import com.eventitta.post.service.PostService;
import com.eventitta.region.controller.RegionController;
import com.eventitta.region.service.RegionService;
import com.eventitta.user.controller.UserController;
import com.eventitta.user.repository.UserRepository;
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
    UserController.class,
    RegionController.class
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
    protected JwtAuthenticationEntryPoint jwtAuthenticationEntryPoint;
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
    @MockitoBean
    protected DiscordNotificationService discordNotificationService;
    @MockitoBean
    protected AlertLevelResolver alertLevelResolver;
    @MockitoBean
    protected UserRepository userRepository;
    @MockitoBean
    protected UserInfoService userInfoService;
    @MockitoBean
    protected RegionService regionService;
    @MockitoBean
    protected FileValidationService fileValidationService;
}
