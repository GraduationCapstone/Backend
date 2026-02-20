package com.graduationCapstone.Probe.domain.github.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "GitHub에서 조회한 파일 정보")
public record GithubFileDto(
        @Schema(description = "파일 이름 (확장자 포함)", example = "Probe.java")
        String fileName,

        @Schema(description = "파일 내용(코드)")
        String content
) {
}
