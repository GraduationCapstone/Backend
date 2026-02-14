package com.graduationCapstone.Probe.domain.project.dto;

import com.graduationCapstone.Probe.domain.project.entity.Project;

public record ProjectResponseDto(
        Long id,
        String projectName,
        int memberCount,
        int repoCount
) {
    public static ProjectResponseDto from(Project project) {
        return new ProjectResponseDto(
                project.getId(),
                project.getProjectName(),
                project.getMembers().size(),
                project.getProjectRepos().size()
        );
    }
}