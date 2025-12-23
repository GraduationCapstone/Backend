package com.graduationCapstone.Probe.global.security.login.service;

import com.graduationCapstone.Probe.global.security.jwt.util.JwtUtil;
import com.graduationCapstone.Probe.global.security.login.dto.TokenResponseDto;
import com.graduationCapstone.Probe.global.security.login.entity.RefreshToken;
import com.graduationCapstone.Probe.global.security.login.repository.RefreshTokenRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Transactional
public class LoginService {

    private final JwtUtil tokenProvider;
    private final RefreshTokenRepository refreshTokenRepository;

    // 토큰 재발급 로직
    public TokenResponseDto reissue(String refreshToken) {
        if (!tokenProvider.validateToken(refreshToken)) {
            throw new RuntimeException("RefreshToken이 유효하지 않습니다.");
        }

        Long userId = tokenProvider.getUserId(refreshToken); //토큰에서 userId 추출

        RefreshToken storedToken = refreshTokenRepository.findById(userId) // User ID로 RefreshToken 값 가져옴
                .orElseThrow(() -> new RuntimeException("로그아웃 된 사용자입니다."));

        if (!storedToken.getRefreshToken().equals(refreshToken)) {
            throw new RuntimeException("토큰의 유저 정보가 일치하지 않습니다.");
        }

        String newAccessToken = tokenProvider.createAccessToken(userId);
        String newRefreshToken = tokenProvider.createRefreshToken(userId);

        storedToken.updateToken(newRefreshToken);

        return new TokenResponseDto(newAccessToken, newRefreshToken);
    }

    // 로그아웃 로직
    public void logout(String accessToken) {
        Long userId = tokenProvider.getUserId(accessToken);
        refreshTokenRepository.deleteById(userId);
    }
}
