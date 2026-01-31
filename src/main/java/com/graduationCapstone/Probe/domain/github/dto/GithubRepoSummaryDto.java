package com.graduationCapstone.Probe.domain.github.dto;

public record GithubRepoSummaryDto(
        String name,
        String owner,
        String description,
        String language,
        int forksCount,
        int stargazersCount,
        int openIssuesCount
) {
}