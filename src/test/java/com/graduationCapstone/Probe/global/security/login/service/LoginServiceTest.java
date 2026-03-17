package com.graduationCapstone.Probe.global.security.login.service;

import com.graduationCapstone.Probe.global.security.util.CookieUtil;
import org.junit.jupiter.api.BeforeEach;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import com.graduationCapstone.Probe.global.exception.ErrorCode;
import com.graduationCapstone.Probe.global.exception.handler.CustomException;
import com.graduationCapstone.Probe.global.security.jwt.util.JwtUtil;
import com.graduationCapstone.Probe.global.security.login.dto.TokenResponseDto;
import com.graduationCapstone.Probe.global.security.login.entity.RefreshToken;
import com.graduationCapstone.Probe.global.security.login.repository.RefreshTokenRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class LoginServiceTest {

    @InjectMocks
    private LoginService loginService;

    @Mock
    private RefreshTokenRepository refreshTokenRepository;

    @Mock
    private JwtUtil jwtUtil;

    @Mock
    private CookieUtil cookieUtil;

    private MockHttpServletResponse response;

    @BeforeEach
    void setUp() {
        response = new MockHttpServletResponse();
    }

    @Test
    @DisplayName("토큰 재발급 성공: 새 토큰을 쿠키에 추가하는 메서드가 호출되어야 합니다.")
    void reissue_Success() {
        // given
        String oldRefreshToken = "old-refresh-token";
        Long userId = 1L;
        String newAccessToken = "new-access-token";
        String newRefreshToken = "new-refresh-token";

        given(jwtUtil.validateToken(oldRefreshToken)).willReturn(true);
        given(jwtUtil.getUserId(oldRefreshToken)).willReturn(userId);

        // DB에서 userID로 저장된 토큰 찾기
        RefreshToken storedToken = RefreshToken.builder()
                .userId(userId)
                .refreshToken(oldRefreshToken)
                .build();
        given(refreshTokenRepository.findByUserId(userId)).willReturn(Optional.of(storedToken));

        // 새 토큰 생성 설정
        given(jwtUtil.createAccessToken(userId)).willReturn(newAccessToken);
        given(jwtUtil.createRefreshToken(userId)).willReturn(newRefreshToken);

        // when
        TokenResponseDto result = loginService.reissue(oldRefreshToken, response);

        // then
        assertThat(result.accessToken()).isEqualTo(newAccessToken);
        // CookieUtil이 제대로 호출되었는지 검증
        verify(cookieUtil).addAccessCookie(response, newAccessToken);
        verify(cookieUtil).addRefreshCookie(response, newRefreshToken);
    }

    @Test
    @DisplayName("재발급 실패: 유효하지 않은 토큰이면 쿠키 삭제 메서드가 호출되어야 합니다.")
    void reissue_Fail_InvalidToken() {
        // given
        String invalidToken = "invalid-token";

        // 유효성 검사 실패 설정
        given(jwtUtil.validateToken(invalidToken)).willReturn(false);

        // when & then
        assertThatThrownBy(() -> loginService.reissue(invalidToken, response))
                .isInstanceOf(CustomException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.INVALID_REFRESH_TOKEN);

        verify(cookieUtil).deleteRefreshCookie(response);
    }

    @Test
    @DisplayName("재발급 실패: DB에 해당 사용자의 토큰 정보가 없으면(이미 로그아웃됨) 예외가 발생합니다.")
    void reissue_Fail_LoggedOut() {
        // given
        String refreshToken = "valid-token";
        Long userId = 1L;

        given(jwtUtil.validateToken(refreshToken)).willReturn(true);
        given(jwtUtil.getUserId(refreshToken)).willReturn(userId);

        // DB 조회 결과가 Empty
        given(refreshTokenRepository.findById(userId)).willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> loginService.reissue(refreshToken, response))
                .isInstanceOf(CustomException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.REFRESH_TOKEN_NOT_FOUND);
    }

    @Test
    @DisplayName("재발급 실패: DB 토큰과 미일치 시 쿠키 삭제 메서드가 호출되어야 합니다.")
    void reissue_Fail_Mismatch() {
        // given
        String requestToken = "hacker-token";
        Long userId = 1L;

        given(jwtUtil.validateToken(requestToken)).willReturn(true);
        given(jwtUtil.getUserId(requestToken)).willReturn(userId);

        // DB에는 "original-token"이 들어있음 (요청 토큰과 다름)
        RefreshToken storedToken = RefreshToken.builder()
                .userId(userId)
                .refreshToken("original-token")
                .build();
        given(refreshTokenRepository.findByUserId(userId)).willReturn(Optional.of(storedToken));

        // when & then
        assertThatThrownBy(() -> loginService.reissue(requestToken, response))
                .isInstanceOf(CustomException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.REFRESH_TOKEN_MISMATCH);

        verify(cookieUtil).deleteRefreshCookie(response);
    }

    @Test
    @DisplayName("로그아웃 성공: DB에서 삭제하고 모든 쿠키 삭제 메서드(deleteAll)를 호출해야 합니다.")
    void logout_Success() {
        // given
        MockHttpServletRequest request = new MockHttpServletRequest();
        String accessToken = "valid-access";
        Long userId = 1L;

        // CookieUtil에서 AccessToken을 꺼내오는 상황 Mocking
        given(cookieUtil.getCookieValue(any(), eq(CookieUtil.ACCESS_TOKEN_COOKIE_NAME))).willReturn(accessToken);
        given(jwtUtil.validateToken(accessToken)).willReturn(true);
        given(jwtUtil.getUserId(accessToken)).willReturn(userId);

        // when
        loginService.logout(request, response);

        // then
        verify(refreshTokenRepository).deleteByUserId(userId);
        verify(cookieUtil).deleteAllCookies(response);
    }
}