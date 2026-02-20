package com.graduationCapstone.Probe.domain.project.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.util.List;

@Schema(description = "프로젝트 생성 요청 DTO")
public record ProjectCreateRequestDto(
        @Schema(description = "생성할 프로젝트의 이름 (최대 20자)")
        @NotBlank(message = "프로젝트 이름은 필수입니다.")
        @Size(max = 20, message = "프로젝트 이름은 20자 이내여야 합니다.")
        String projectName,

        @Schema(description = "연결할 Github 레포지토리 ID 목록", example = "[1, 2, 3]")
        @NotNull(message = "레포지토리 ID 목록은 null일 수 없습니다. 없을 경우 빈 리스트([])를 보내주세요.")
        List<@Positive(message = "유효하지 않은 레포지토리 ID가 포함되어 있습니다.") Long> repoIds
) {
}