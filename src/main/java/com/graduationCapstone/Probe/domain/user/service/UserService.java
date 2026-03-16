package com.graduationCapstone.Probe.domain.user.service;

import com.graduationCapstone.Probe.domain.user.entity.User;
import com.graduationCapstone.Probe.domain.user.repository.UserRepository;
import com.graduationCapstone.Probe.global.exception.ErrorCode;
import com.graduationCapstone.Probe.global.exception.handler.CustomException;
import com.graduationCapstone.Probe.global.security.login.repository.RefreshTokenRepository;
import com.graduationCapstone.Probe.global.security.oauth.dto.OAuth2ResponseDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final RefreshTokenRepository refreshTokenRepository;

    /**
     * OAuth2 제공자(GitHub)로부터 받은 정보를 바탕으로 사용자를 저장하거나 기존 정보를 업데이트합니다.
     * 신규 사용자는 등록 처리하며, 탈퇴한 사용자가 재로그인 시 계정을 재활성화합니다.
     *
     * @param attributes OAuth2 인증 과정에서 추출된 사용자 프로필 정보 DTO
     * @return 저장 또는 업데이트가 완료된 User 엔티티
     */
    @Transactional
    public User saveOrUpdateUser(OAuth2ResponseDto attributes) { //CustomOAuth2UserService에서 User 객체 바로 사용할 수 있도록 void -> user 로 리턴 타입 수정
        log.info("OAuth2 유저 정보 저장/업데이트: githubId={}, username={}", attributes.githubId(), attributes.username());
        Optional<User> existingUser = userRepository.findByGithubId(attributes.githubId());

        User user;

        if (existingUser.isPresent()) {
            user = existingUser.get();

            // 탈퇴 계정이라면 재활성화
            if (user.isDeleted()) {
                log.info("탈퇴 계정 재활성화: userId={}", user.getId());
                user.reactivate();
                user.updateUsername(attributes.username());
                user.updateProfileImageUrl(attributes.profileImageUrl());
            } else {
                log.debug("기존 사용자 정보 업데이트 진행: userId={}", user.getId());
                // 기존 사용자: 닉네임과 프로필 이미지 모두 최신 상태로 업데이트
                user.updateUsername(attributes.username());
                user.updateProfileImageUrl(attributes.profileImageUrl());
            }
        } else {
            log.info("신규 사용자 등록 진행: githubId={}", attributes.githubId());
            // 신규 사용자
            user = attributes.toEntity();
        }

        return userRepository.save(user);
    }

    /**
     * 사용자의 계정을 탈퇴 처리합니다 (논리적 삭제).
     * 데이터베이스의 레코드는 유지하되 삭제 플래그(isDeleted)를 설정하고, 해당 사용자의 모든 RefreshToken을 제거합니다.
     *
     * @param userId 탈퇴 처리를 진행할 사용자의 PK
     * @throws CustomException USER_NOT_FOUND 사용자를 찾을 수 없는 경우 발생
     */
    @Transactional
    public void deleteUser(Long userId){
        log.info("회원 탈퇴 처리 시작: userId={}", userId);
        User user = userRepository.findById(userId)
                .orElseThrow(() -> {
                    log.error("탈퇴 실패: 사용자를 찾을 수 없음 - userId={}", userId);
                    return new CustomException(ErrorCode.USER_NOT_FOUND);
                });

        refreshTokenRepository.deleteByUserId(userId);
        user.deleted(true);
        log.info("회원 탈퇴 처리 완료: userId={}", userId);
    }

    /**
     * 사용자의 GitHub API 접근용 AccessToken을 최신 상태로 갱신합니다.
     * GitHub API 호출 시 권한 검증에 사용됩니다.
     *
     * @param userId 토큰을 갱신할 사용자의 PK
     * @param token  GitHub로부터 발급받은 새로운 AccessToken
     * @return 토큰 갱신이 완료된 User 엔티티
     */
    @Transactional
    public User updateGithubToken(Long userId, String token) {
        log.debug("GitHub AccessToken 업데이트: userId={}", userId);
        User user = userRepository.findById(userId)
                .orElseThrow(() -> {
                    log.error("GitHub 토큰 업데이트 실패: 존재하지 않는 사용자 - userId={}", userId);
                    return new CustomException(ErrorCode.USER_NOT_FOUND);
                });
        user.updateGithubAccessToken(token);
        log.info("GitHub AccessToken 업데이트 완료: userId={}", userId);
        return userRepository.saveAndFlush(user);
    }

}