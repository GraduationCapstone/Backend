package com.graduationCapstone.Probe.domain.github.dto;

public record GithubRepoResponseDto(
        Long id,
        String repoName,
        String owner,
        String description,
        String language,
        int forksCount,
        int stargazersCount,
        int openIssuesCount
) {
}
