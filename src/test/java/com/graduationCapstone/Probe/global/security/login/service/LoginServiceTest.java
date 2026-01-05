package com.graduationCapstone.Probe.global.security.login.service;

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

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
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

    @Test
    @DisplayName("토큰 재발급 성공: 유효한 RT가 들어오면 새로운 AT, RT를 발급하고 DB를 업데이트합니다.")
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
        TokenResponseDto result = loginService.reissue(oldRefreshToken);

        // then
        // 반환된 DTO 값이 새 토큰인지 확인
        assertThat(result.accessToken()).isEqualTo(newAccessToken);
        assertThat(result.refreshToken()).isEqualTo(newRefreshToken);

        // DB에서 가져온 Entity의 updateToken 메서드가 호출되어 값이 바뀌었는지 확인
        assertThat(storedToken.getRefreshToken()).isEqualTo(newRefreshToken);
    }

    @Test
    @DisplayName("재발급 실패: Refresh Token이 유효하지 않으면 예외가 발생합니다.")
    void reissue_Fail_InvalidToken() {
        // given
        String invalidToken = "invalid-token";

        // 유효성 검사 실패 설정
        given(jwtUtil.validateToken(invalidToken)).willReturn(false);

        // when & then
        assertThatThrownBy(() -> loginService.reissue(invalidToken))
                .isInstanceOf(CustomException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.INVALID_REFRESH_TOKEN);
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
        assertThatThrownBy(() -> loginService.reissue(refreshToken))
                .isInstanceOf(CustomException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.REFRESH_TOKEN_NOT_FOUND);
    }

    @Test
    @DisplayName("재발급 실패: DB에 저장된 토큰과 요청한 토큰이 다르면 예외가 발생합니다.")
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
        assertThatThrownBy(() -> loginService.reissue(requestToken))
                .isInstanceOf(CustomException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.REFRESH_TOKEN_MISMATCH);
    }

    @Test
    @DisplayName("로그아웃 성공: 토큰에서 ID를 추출해 DB 삭제 메서드를 호출합니다.")
    void logout_Success() {
        // given
        String accessToken = "valid-access-token";
        Long userId = 1L;

        given(jwtUtil.getUserId(accessToken)).willReturn(userId);

        // when
        loginService.logout(accessToken);

        // then
        verify(refreshTokenRepository).deleteByUserId(userId);
    }
}