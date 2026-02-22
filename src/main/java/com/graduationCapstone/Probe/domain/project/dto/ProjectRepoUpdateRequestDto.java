package com.graduationCapstone.Probe.domain.project.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Positive;

import java.util.List;

@Schema(description = "프로젝트 레포지토리 수정 요청 DTO")
public record ProjectRepoUpdateRequestDto(
        @Schema(description = "새롭게 설정할 레포지토리 ID 목록 (최소 1개 이상)", example = "[1, 2, 3]", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotEmpty(message = "레포지토리 목록은 비어있을 수 없습니다.")
        List<@Positive(message = "유효하지 않은 레포지토리 ID가 포함되어 있습니다.") Long> repoIds
) {
}