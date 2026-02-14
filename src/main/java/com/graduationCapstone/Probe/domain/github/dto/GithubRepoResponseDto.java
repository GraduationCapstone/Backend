package com.graduationCapstone.Probe.domain.github.dto;

import com.graduationCapstone.Probe.domain.github.entity.GithubRepo;

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
