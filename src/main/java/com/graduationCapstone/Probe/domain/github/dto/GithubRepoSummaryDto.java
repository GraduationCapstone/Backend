package com.graduationCapstone.Probe.domain.github.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;

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
        int openIssuesCount,

        @Schema(description = "public, private 여부")
        boolean isPublic,

        @Schema(description = "업데이트 날짜")
        // { 수정: 프론트엔드의 ISO 8601 형식을 LocalDateTime으로 파싱 }
        @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss.SSSX")
        LocalDateTime updatedAt
) {
}