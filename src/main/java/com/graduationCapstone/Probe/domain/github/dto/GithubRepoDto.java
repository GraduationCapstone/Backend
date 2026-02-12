package com.graduationCapstone.Probe.domain.github.dto;

import java.util.List;

public record GithubRepoDto(
        String repoName,
        List<GithubFileDto> files
) {
}
