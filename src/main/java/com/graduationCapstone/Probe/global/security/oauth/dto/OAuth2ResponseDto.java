package com.graduationCapstone.Probe.global.security.oauth.dto;

import com.graduationCapstone.Probe.domain.user.entity.User;

import java.util.Map;


public record OAuth2ResponseDto (
        Map<String, Object> attributes,
        String nameAttributeKey,
        String githubId,
        String username,
        String email
){

    /**
     * 외부에서 들어온 데이터 OAuth2ResponseDto로 변환
     */
    public static OAuth2ResponseDto of (String registrationId, String userNameAttributeName, Map<String, Object> attributes) {

        if ("github".equals(registrationId)) {
            return ofGithub(userNameAttributeName, attributes);
        }

        // 지원하지 않는 Provider가 넘어올 경우 예외 처리
        throw new IllegalArgumentException("Unsupported OAuth Provider: " + registrationId);
    }

    /**
     * GitHub 전용 사용자 정보 추출 로직
     */
    private static OAuth2ResponseDto ofGithub(String userNameAttributeName, Map<String, Object> attributes) {

        String extractedGithubId = String.valueOf(attributes.get(userNameAttributeName));
        String extractedUsername = (String) attributes.get("login");
        String extractedEmail = (String) attributes.get("email");

        return new OAuth2ResponseDto(
                attributes,
                userNameAttributeName,
                extractedGithubId,
                extractedUsername,
                extractedEmail
        );
    }

    /**
     * DTO 데이터를 기반으로 User 엔티티 생성 (신규 가입 시 사용)
     */
    public User toEntity() {
        return User.builder()
                .githubId(this.githubId)
                .username(this.username)
                .email(this.email)
                .build();
    }
}