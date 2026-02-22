package com.graduationCapstone.Probe.domain.github.dto;

import io.swagger.v3.oas.annotations.media.Schema;

public record GithubRepoSummaryDto(
        @Schema(description = "레포지토리 이름", example = "Probe-Project", requiredMode = Schema.RequiredMode.REQUIRED)
        String repoName,

        @Schema(description = "레포지토리 소유자", example = "MAKC", requiredMode = Schema.RequiredMode.REQUIRED)
        String owner,

        @Schema(description = "레포지토리 설명")
        String description,

        @Schema(description = "주요 사용 언어", example = "Java")
        String language,

        @Schema(description = "포크 수")
        int forksCount,

        @Schema(description = "스타 수")
        int stargazersCount,

        @Schema(description = "오픈된 이슈 수")
        int openIssuesCount
) {
}