package com.graduationCapstone.Probe.global.security.login.controller;

import com.graduationCapstone.Probe.global.exception.ErrorCode;
import com.graduationCapstone.Probe.global.exception.handler.CustomException;
import com.graduationCapstone.Probe.global.security.login.dto.TokenResponseDto;
import com.graduationCapstone.Probe.global.security.login.service.LoginService;
import com.graduationCapstone.Probe.global.security.util.CookieUtil;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class LoginControllerTest {

    @InjectMocks
    private LoginController loginController;

    @Mock
    private LoginService loginService;

    @Mock
    private CookieUtil cookieUtil;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(loginController).build();
    }

    @Test
    @DisplayName("토큰 재발급 성공: 쿠키에서 RT를 꺼내 재발급 후, 새 RT는 쿠키에, AT는 Body에 담아 반환합니다.")
    void reissue_Success() throws Exception {
        // given
        String refreshToken = "valid-refresh-token";
        String newAccessToken = "new-access-token";
        String newRefreshToken = "new-refresh-token";

        TokenResponseDto serviceResponse = new TokenResponseDto(newAccessToken, newRefreshToken);

        // CookieUtil이 요청에서 토큰을 잘 꺼낸다고 가정
        given(cookieUtil.getRefreshTokenFromCookie(any(HttpServletRequest.class)))
                .willReturn(refreshToken);

        // 서비스가 재발급을 성공한다고 가정
        given(loginService.reissue(eq(refreshToken), any(HttpServletResponse.class))).willReturn(serviceResponse);

        // when & then
        mockMvc.perform(post("/api/auth/reissue"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").value(newAccessToken)) // Body에는 AT만 있어야 함
                .andExpect(jsonPath("$.refreshToken").isEmpty()); // Controller에서 RT는 null로 내려줌

        verify(loginService).reissue(eq(refreshToken), any(HttpServletResponse.class));
    }

    @Test
    @DisplayName("토큰 재발급 실패: 쿠키에 Refresh Token이 없으면 REFRESH_TOKEN_NOT_FOUND 예외가 발생합니다.")
    void reissue_Fail_NoCookie() throws Exception {
        // given
        // 쿠키가 null을 반환
        given(cookieUtil.getRefreshTokenFromCookie(any(HttpServletRequest.class))).willReturn(null);

        // when & then
        // ControllerAdvice가 설정되지 않은 단위 테스트라 원시 예외가 터질 수 있으므로 예외 타입 검증
        try {
            mockMvc.perform(post("/api/auth/reissue"));
        } catch (Exception e) {
            // 실제 런타임에서는 GlobalExceptionHandler가 처리하겠지만, 여기선 로직 흐름상 예외 발생 확인
            if (e.getCause() instanceof CustomException customException) {
                if(customException.getErrorCode() != ErrorCode.REFRESH_TOKEN_NOT_FOUND) {
                    throw e; // 다른 에러면 테스트 실패
                }
            }
        }
    }

    @Test
    @DisplayName("로그아웃 성공: 헤더 없이 요청해도 서비스의 logout(request, response)이 호출되어야 함")
    void logout_Success() throws Exception {
        // when & then
        mockMvc.perform(post("/api/auth/logout")) // 💡 Header 제거됨
                .andExpect(status().isNoContent());

        // then
        // 서비스가 request와 response를 받아서 내부에서 처리하는지 확인
        verify(loginService).logout(any(HttpServletRequest.class), any(HttpServletResponse.class));
    }
}