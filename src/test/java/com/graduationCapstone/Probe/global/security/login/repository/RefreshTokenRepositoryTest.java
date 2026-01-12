package com.graduationCapstone.Probe.global.security.login.repository;

import com.graduationCapstone.Probe.global.security.login.entity.RefreshToken;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
class RefreshTokenRepositoryTest {

    @Autowired
    private RefreshTokenRepository refreshTokenRepository;

    @Test
    @DisplayName("userId로 토큰을 조회하고 삭제할 수 있어야 합니다.")
    void saveAndFindAndDelete() {
        // 저장
        Long userId = 1L;
        RefreshToken token = RefreshToken.builder()
                .userId(userId)
                .refreshToken("test-token")
                .build();

        refreshTokenRepository.save(token);

        // 조회
        Optional<RefreshToken> foundToken = refreshTokenRepository.findByUserId(userId);
        assertThat(foundToken).isPresent();
        assertThat(foundToken.get().getRefreshToken()).isEqualTo("test-token");

        // 삭제 - 커스텀 메서드 테스트
        refreshTokenRepository.deleteByUserId(userId);

        // 삭제 확인
        Optional<RefreshToken> deletedToken = refreshTokenRepository.findByUserId(userId);
        assertThat(deletedToken).isEmpty();
    }
}