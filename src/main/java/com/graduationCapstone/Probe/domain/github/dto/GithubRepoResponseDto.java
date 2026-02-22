package com.graduationCapstone.Probe.domain.github.dto;

import com.graduationCapstone.Probe.domain.github.entity.GithubRepo;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "GitHub 레포지토리 정보 응답 DTO")
public record GithubRepoResponseDto(
        @Schema(description = "레포지토리 고유 ID", example = "1", requiredMode = Schema.RequiredMode.REQUIRED)
        Long id,

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
    public static GithubRepoResponseDto from(GithubRepo repo) {
        return new GithubRepoResponseDto(
                repo.getId(),
                repo.getRepoName(),
                repo.getOwner(),
                repo.getDescription(),
                repo.getLanguage(),
                repo.getForksCount(),
                repo.getStargazersCount(),
                repo.getOpenIssuesCount()
        );
    }
}
