package com.graduationCapstone.Probe.domain.user.service;

import com.graduationCapstone.Probe.domain.user.entity.User;
import com.graduationCapstone.Probe.domain.user.repository.UserRepository;
import com.graduationCapstone.Probe.global.exception.ErrorCode;
import com.graduationCapstone.Probe.global.exception.handler.CustomException;
import com.graduationCapstone.Probe.global.security.login.repository.RefreshTokenRepository;
import com.graduationCapstone.Probe.global.security.oauth.dto.OAuth2ResponseDto;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final RefreshTokenRepository refreshTokenRepository;

    /**
     * OAuth2 정보를 기반으로 사용자를 저장하거나 업데이트하는 로직을 수행합니다.
     * 이 메서드는 CustomOAuth2UserService에서 호출됩니다.
     * @param attributes OAuth2 Provider에서 가져온 사용자 정보 DTO
     */
    @Transactional
    public void saveOrUpdateUser(OAuth2ResponseDto attributes) {

        Optional<User> existingUser = userRepository.findByGithubId(attributes.githubId());

        User user;

        if (existingUser.isPresent()) {
            user = existingUser.get();

            // 탈퇴 계정이라면 재활성화
            if (user.isDeleted()) {
                user.reactivate();
                user.updateUsername(attributes.username());
            } else {
                // 기존 사용자: 닉네임만 업데이트
                user.updateUsername(attributes.username());
            }
        } else {
            // 신규 사용자
            user = attributes.toEntity();
        }

        userRepository.save(user);
    }

    @Transactional
    public void deleteUser(Long userId){

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));

        refreshTokenRepository.deleteByUserId(userId);

        user.deleted(true);
    }

}