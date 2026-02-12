package com.graduationCapstone.Probe.domain.github.dto;

public record GithubRepoSummaryDto(
        String repoName,
        String owner,
        String description,
        String language,
        int forksCount,
        int stargazersCount,
        int openIssuesCount
) {
}