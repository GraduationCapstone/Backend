package com.graduationCapstone.Probe.domain.project.dto;

import com.graduationCapstone.Probe.domain.project.entity.Project;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "프로젝트 정보 응답 DTO")
public record ProjectResponseDto(
        @Schema(description = "프로젝트 고유 ID", example = "1", requiredMode = Schema.RequiredMode.REQUIRED)
        Long id,

        @Schema(description = "프로젝트 이름", requiredMode = Schema.RequiredMode.REQUIRED)
        String projectName,

        @Schema(description = "프로젝트 참여 멤버 수", example = "3", requiredMode = Schema.RequiredMode.REQUIRED)
        int memberCount,

        @Schema(description = "프로젝트에 연결된 레포지토리 수", example = "2", requiredMode = Schema.RequiredMode.REQUIRED)
        int repoCount
) {
    public static ProjectResponseDto from(Project project) {
        return new ProjectResponseDto(
                project.getId(),
                project.getProjectName(),
                project.getMembers()!=null?project.getMembers().size():0,
                project.getProjectRepos()!=null?project.getProjectRepos().size():0
        );
    }

    public static ProjectResponseDto of(Project project, int memberCount, int repoCount) {
        return new ProjectResponseDto(
                project.getId(),
                project.getProjectName(),
                memberCount,
                repoCount
        );
    }
}