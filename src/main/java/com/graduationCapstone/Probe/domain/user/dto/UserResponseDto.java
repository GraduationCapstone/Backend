package com.graduationCapstone.Probe.domain.user.dto;

import com.graduationCapstone.Probe.domain.user.entity.User;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "내 정보 조회 응답 데이터")
public record UserResponseDto(
        @Schema(description = "사용자 닉네임", example = "MAKC")
        String username,

        @Schema(description = "사용자 이메일", example = "probe@gmail.com")
        String email,

        @Schema(description = "GitHub 프로필 이미지 URL")
        String profileImageUrl
) {
    public static UserResponseDto from(User user) {
        return new UserResponseDto(
                user.getUsername(),
                user.getEmail(),
                user.getProfileImageUrl()
        );
    }
}