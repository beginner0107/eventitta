package com.eventitta;

import com.eventitta.api.auth.controller.AuthController;
import com.eventitta.api.auth.jwt.JwtAccessDeniedHandler;
import com.eventitta.api.auth.jwt.JwtAuthenticationEntryPoint;
import com.eventitta.api.auth.jwt.JwtTokenProvider;
import com.eventitta.api.auth.config.SecurityConfig;
import com.eventitta.api.auth.mapper.AuthMapperImpl;
import com.eventitta.api.auth.jwt.service.CustomUserDetailsService;
import com.eventitta.api.auth.jwt.service.UserInfoService;
import com.eventitta.api.auth.properties.SecurityCorsProperties;
import com.eventitta.api.auth.web.ClientSessionMetadataResolver;
import com.eventitta.api.auth.web.CookieManager;
import com.eventitta.api.auth.web.KakaoAuthorizationSupport;
import com.eventitta.api.comment.controller.CommentController;
import com.eventitta.domain.comment.service.CommentService;
import com.eventitta.domain.auth.service.AuthService;
import com.eventitta.domain.notification.resolver.AlertLevelResolver;
import com.eventitta.domain.notification.service.AlertNotificationService;
import com.eventitta.api.file.controller.FileUploadController;
import com.eventitta.domain.auth.repository.RefreshTokenRepository;
import com.eventitta.domain.file.api.internal.facade.FileStorageFacade;
import com.eventitta.domain.file.service.FileValidationService;
import com.eventitta.domain.gamification.service.GamificationQueryService;
import com.eventitta.domain.media.service.MediaAssetService;
import com.eventitta.api.post.controller.PostController;
import com.eventitta.domain.post.service.PostService;
import com.eventitta.api.region.controller.RegionController;
import com.eventitta.domain.region.service.RegionService;
import com.eventitta.api.user.controller.UserController;
import com.eventitta.api.user.mapper.UserMapperImpl;
import com.eventitta.domain.user.api.internal.facade.UserInternalFacade;
import com.eventitta.domain.user.repository.UserRepository;
import com.eventitta.domain.user.service.UserService;
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
@Import({SecurityConfig.class, AuthMapperImpl.class, UserMapperImpl.class})
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
    protected JwtAuthenticationEntryPoint jwtAuthenticationEntryPoint;
    @MockitoBean
    protected JwtAccessDeniedHandler jwtAccessDeniedHandler;
    @MockitoBean
    protected CookieManager cookieManager;
    @MockitoBean
    protected KakaoAuthorizationSupport kakaoAuthorizationSupport;
    @MockitoBean
    protected ClientSessionMetadataResolver clientSessionMetadataResolver;
    @MockitoBean
    protected SecurityCorsProperties securityCorsProperties;
    @MockitoBean
    protected PostService postService;
    @MockitoBean
    protected FileStorageFacade storageService;
    @MockitoBean
    protected MediaAssetService mediaAssetService;
    @MockitoBean
    protected CommentService commentService;
    @MockitoBean
    protected UserService userService;
    @MockitoBean
    protected GamificationQueryService gamificationQueryService;
    @MockitoBean
    protected AlertNotificationService alertNotificationService;
    @MockitoBean
    protected AlertLevelResolver alertLevelResolver;
    @MockitoBean
    protected UserRepository userRepository;
    @MockitoBean
    protected UserInfoService userInfoService;
    @MockitoBean
    protected UserInternalFacade userInternalFacade;
    @MockitoBean
    protected RefreshTokenRepository refreshTokenRepository;
    @MockitoBean
    protected RegionService regionService;
    @MockitoBean
    protected FileValidationService fileValidationService;
}
