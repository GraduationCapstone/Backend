package com.graduationCapstone.Probe.global.security.login.service;

import com.graduationCapstone.Probe.global.exception.ErrorCode;
import com.graduationCapstone.Probe.global.exception.handler.CustomException;
import com.graduationCapstone.Probe.global.security.jwt.util.JwtUtil;
import com.graduationCapstone.Probe.global.security.login.dto.TokenResponseDto;
import com.graduationCapstone.Probe.global.security.login.entity.RefreshToken;
import com.graduationCapstone.Probe.global.security.login.repository.RefreshTokenRepository;
import com.graduationCapstone.Probe.global.security.util.CookieUtil;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class LoginService {

    private final JwtUtil jwtUtil;
    private final RefreshTokenRepository refreshTokenRepository;
    private final CookieUtil cookieUtil;

    // 토큰 재발급 로직
    public TokenResponseDto reissue(String refreshToken, HttpServletResponse response) {
        log.info("토큰 재발급 요청 시작");
        if (!jwtUtil.validateToken(refreshToken)) {
            log.warn("토큰 재발급 실패: 유효하지 않은 리프레시 토큰");
            cookieUtil.deleteRefreshCookie(response);
            throw new CustomException(ErrorCode.INVALID_REFRESH_TOKEN);
        }

        Long userId = jwtUtil.getUserId(refreshToken); //토큰에서 userId 추출

        RefreshToken storedToken = refreshTokenRepository.findByUserId(userId) // User ID로 RefreshToken 값 가져옴
                .orElseThrow(() -> {
                    log.warn("토큰 재발급 실패: DB에 리프레시 토큰이 없음 (userId={})", userId);
                    cookieUtil.deleteRefreshCookie(response);
                    return new CustomException(ErrorCode.REFRESH_TOKEN_NOT_FOUND);
                });

        if (!storedToken.getRefreshToken().equals(refreshToken)) {
            log.warn("토큰 재발급 실패: 토큰 불일치 (userId={})", userId);
            cookieUtil.deleteRefreshCookie(response);
            throw new CustomException(ErrorCode.REFRESH_TOKEN_MISMATCH);
        }

        String newAccessToken = jwtUtil.createAccessToken(userId);
        String newRefreshToken = jwtUtil.createRefreshToken(userId);

        storedToken.updateToken(newRefreshToken);

        cookieUtil.addAccessCookie(response, newAccessToken);
        cookieUtil.addRefreshCookie(response, newRefreshToken);

        log.info("토큰 재발급 완료: userId={}", userId);
        return new TokenResponseDto(newAccessToken, newRefreshToken);
    }

    // 로그아웃 로직
    public void logout(HttpServletRequest request, HttpServletResponse response) {
        String accessToken = cookieUtil.getCookieValue(request, CookieUtil.ACCESS_TOKEN_COOKIE_NAME);
        String refreshToken = cookieUtil.getCookieValue(request, CookieUtil.REFRESH_TOKEN_COOKIE_NAME);

        Long userId = null;

        if (accessToken != null && jwtUtil.validateToken(accessToken)) {
            userId = jwtUtil.getUserId(accessToken);
        }

        if (userId == null && refreshToken != null && jwtUtil.validateToken(refreshToken)) {
            userId = jwtUtil.getUserId(refreshToken);
        }

        if (userId != null) {
            log.info("로그아웃 DB 처리 진행: userId={}", userId);
            refreshTokenRepository.deleteByUserId(userId);
        } else {
            log.warn("로그아웃 시 유효한 토큰이 없어 DB 삭제를 스킵합니다. (쿠키만 삭제)");
        }

        cookieUtil.deleteAllCookies(response);
        SecurityContextHolder.clearContext(); // 현재 스레드에 남아있을지 모르는 인증 정보 확실히 날림
    }
}
