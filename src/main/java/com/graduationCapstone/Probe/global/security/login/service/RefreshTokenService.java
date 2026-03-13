package com.graduationCapstone.Probe.global.security.login.service;

import com.graduationCapstone.Probe.global.security.login.dto.RefreshTokenSaveDto;
import com.graduationCapstone.Probe.global.security.login.entity.RefreshToken;
import com.graduationCapstone.Probe.global.security.login.repository.RefreshTokenRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class RefreshTokenService {

    private final RefreshTokenRepository refreshTokenRepository;

    public void saveOrUpdate(RefreshTokenSaveDto dto) {
        log.debug("리프레시 토큰 저장/업데이트 시도: userId={}", dto.userId());
        refreshTokenRepository.findByUserId(dto.userId())
                .ifPresentOrElse(
                        r -> r.updateToken(dto.refreshToken()),
                        () -> {
                            log.info("신규 리프레시 토큰 저장: userId={}", dto.userId());
                            refreshTokenRepository.save(
                                    RefreshToken.builder()
                                            .userId(dto.userId())
                                            .refreshToken(dto.refreshToken())
                                            .build()
                            );
                        }
                );
    }
}
