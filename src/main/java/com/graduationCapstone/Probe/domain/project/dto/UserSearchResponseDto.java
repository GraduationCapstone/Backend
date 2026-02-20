package com.graduationCapstone.Probe.domain.project.dto;

import com.graduationCapstone.Probe.domain.user.entity.User;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "사용자 검색 결과 응답 DTO")
public record UserSearchResponseDto(
        @Schema(description = "사용자 고유 ID", example = "1", requiredMode = Schema.RequiredMode.REQUIRED)
        Long userId,

        @Schema(description = "사용자 이름(닉네임)", example = "MAKC", requiredMode = Schema.RequiredMode.REQUIRED)
        String username,

        @Schema(description = "사용자 이메일", example = "probe@gmail.com", requiredMode = Schema.RequiredMode.REQUIRED)
        String email
) {
    public static UserSearchResponseDto from(User user) {
        return new UserSearchResponseDto(
                user.getId(),
                user.getUsername(),
                user.getEmail()
        );
    }
}