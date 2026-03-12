package com.graduationCapstone.Probe.global.security.oauth.dto;

import com.graduationCapstone.Probe.domain.user.entity.User;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.Map;

@Schema(description = "OAuth2 인증 응답 데이터 정보")
public record OAuth2ResponseDto (
        @Schema(description = "GitHub에서 제공한 원본 속성들")
        Map<String, Object> attributes,

        @Schema(description = "사용자 식별자 키 이름")
        String nameAttributeKey,

        @Schema(description = "GitHub 고유 숫자 ID", example = "583231")
        String githubId,

        @Schema(description = "GitHub 사용자 닉네임", example = "MAKC")
        String username,

        @Schema(description = "GitHub 이메일", example = "probe@gmail.com")
        String email,

        @Schema(description = "GitHub 프로필 이미지 URL")
        String profileImageUrl
){

    /**
     * DTO 데이터를 기반으로 User 엔티티 생성 (신규 가입 시 사용)
     */
    public User toEntity() {
        return User.builder()
                .githubId(this.githubId)
                .username(this.username)
                .email(this.email)
                .profileImageUrl(this.profileImageUrl)
                .build();
    }
}