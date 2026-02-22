package com.graduationCapstone.Probe.domain.project.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

@Schema(description = "Redis 저장용 프로젝트 초대 토큰 정보")
public record InviteTokenInfo(
        @Schema(description = "초대 대상 프로젝트 ID", example = "1")
        @NotNull(message = "프로젝트 ID는 필수입니다.")
        @Positive(message = "유효하지 않은 프로젝트 ID입니다.")
        Long projectId,

        @Schema(description = "초대받을 사용자의 이메일", example = "user@gmail.com")
        @NotBlank(message = "이메일은 필수입니다.")
        @Positive(message = "유효한 이메일 형식이 아닙니다.")
        String inviteEmail) {
}
