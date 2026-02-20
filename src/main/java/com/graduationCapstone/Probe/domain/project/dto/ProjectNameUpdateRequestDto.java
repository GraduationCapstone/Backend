package com.graduationCapstone.Probe.domain.project.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Schema(description = "프로젝트 이름 수정 요청 DTO")
public record ProjectNameUpdateRequestDto(
        @Schema(description = "변경할 새로운 프로젝트 이름", maxLength = 20)
        @NotBlank(message = "변경할 프로젝트 이름은 필수입니다.")
        @Size(max = 20, message = "프로젝트 이름은 20자 이내여야 합니다.")
        String projectName
) {
}
