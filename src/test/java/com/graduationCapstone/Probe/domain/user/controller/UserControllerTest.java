package com.graduationCapstone.Probe.domain.user.controller;

import com.graduationCapstone.Probe.domain.user.entity.User;
import com.graduationCapstone.Probe.domain.user.service.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.MethodParameter;
import org.springframework.http.MediaType;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;

import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class UserControllerTest {

    @InjectMocks
    private UserController userController;

    @Mock
    private UserService userService;

    private MockMvc mockMvc;

    private User mockUser;

    @BeforeEach
    void setUp() {
        // 테스트용 User 객체
        mockUser = User.builder()
                .id(1L)
                .username("testUser")
                .email("test@email.com")
                .githubId("12345")
                .build();

        // @AuthenticationPrincipal을 처리할 가짜 Resolver 생성
        // 이게 없을 시 User user 자리에 null이 들어감
        HandlerMethodArgumentResolver mockAuthPrincipalResolver = new HandlerMethodArgumentResolver() {
            @Override
            public boolean supportsParameter(MethodParameter parameter) {
                return parameter.getParameterAnnotation(AuthenticationPrincipal.class) != null;
            }

            @Override
            public Object resolveArgument(MethodParameter parameter, ModelAndViewContainer mavContainer, NativeWebRequest webRequest, WebDataBinderFactory binderFactory) {
                return mockUser;
            }
        };

        // MockMvc 설정에 Resolver 등록
        mockMvc = MockMvcBuilders.standaloneSetup(userController)
                .setCustomArgumentResolvers(mockAuthPrincipalResolver)
                .build();
    }

    @Test
    @DisplayName("내 정보 조회: 로그인된 사용자 정보를 JSON으로 반환합니다.")
    void getMe_Success() throws Exception {
        // when & then
        mockMvc.perform(get("/api/user/me")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value("testUser"))
                .andExpect(jsonPath("$.email").value("test@email.com"));
    }

    @Test
    @DisplayName("회원 탈퇴: 로그인된 사용자의 ID로 서비스 삭제 메서드를 호출합니다.")
    void deleteMe_Success() throws Exception {
        // when & then
        mockMvc.perform(delete("/api/user/me"))
                .andExpect(status().isNoContent());

        // User 객체에서 ID(1L)를 꺼내서 삭제시켰는지 확인
        verify(userService).deleteUser(1L);
    }
}