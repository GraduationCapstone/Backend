package com.graduationCapstone.Probe.domain.project.dto;

import com.graduationCapstone.Probe.domain.project.entity.ProjectMember;
import com.graduationCapstone.Probe.domain.project.entity.ProjectRole;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "프로젝트 멤버 상세 정보 응답 DTO")
public record ProjectMemberResponseDto(
        @Schema(description = "사용자 고유 ID", example = "1", requiredMode = Schema.RequiredMode.REQUIRED)
        Long userId,

        @Schema(description = "사용자 이름(닉네임)", example = "MAKC", requiredMode = Schema.RequiredMode.REQUIRED)
        String username,

        @Schema(description = "사용자 이메일", example = "probe@gmail.com", requiredMode = Schema.RequiredMode.REQUIRED)
        String email,

        @Schema(description = "GitHub 프로필 이미지 URL", example = "https://...", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
        String profileImageUrl,

        @Schema(description = "프로젝트에서의 권한", example = "OWNER", requiredMode = Schema.RequiredMode.REQUIRED)
        ProjectRole role
) {
    public static ProjectMemberResponseDto from(ProjectMember member) {
        return new ProjectMemberResponseDto(
                member.getUser().getId(),
                member.getUser().getUsername(),
                member.getUser().getEmail(),
                member.getUser().getProfileImageUrl(),
                member.getRole()
        );
    }
}
