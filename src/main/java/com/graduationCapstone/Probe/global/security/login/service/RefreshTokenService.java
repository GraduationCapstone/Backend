package com.graduationCapstone.Probe.global.security.login.service;

import com.graduationCapstone.Probe.global.security.login.dto.RefreshTokenSaveDto;
import com.graduationCapstone.Probe.global.security.login.entity.RefreshToken;
import com.graduationCapstone.Probe.global.security.login.repository.RefreshTokenRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Transactional
public class RefreshTokenService {

    private final RefreshTokenRepository refreshTokenRepository;

    public void saveOrUpdate(RefreshTokenSaveDto dto) {
        refreshTokenRepository.findByUserId(dto.userId())
                .ifPresentOrElse(
                        r -> r.updateToken(dto.refreshToken()),
                        () -> refreshTokenRepository.save(
                                RefreshToken.builder()
                                        .userId(dto.userId())
                                        .refreshToken(dto.refreshToken())
                                        .build()
                        )
                );
    }
}
