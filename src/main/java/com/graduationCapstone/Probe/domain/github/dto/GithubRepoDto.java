package com.graduationCapstone.Probe.domain.github.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

@Schema(description = "GitHub 레포지토리 정보 및 파일 목록 응답")
public record GithubRepoDto(
        @Schema(description = "GitHub 레포지토리 이름")
        String repoName,

        @Schema(description = "레포지토리 내 파일 상세 목록")
        List<GithubFileDto> files
) {
}
