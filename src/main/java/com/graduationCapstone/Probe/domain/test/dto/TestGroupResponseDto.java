package com.graduationCapstone.Probe.domain.test.dto;

import com.graduationCapstone.Probe.domain.test.entity.TestGroup;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

@Schema(description = "테스트 그룹 상세 응답 DTO")
@Builder
public record TestGroupResponseDto(
        @Schema(description = "테스트 그룹 ID")
        Long groupId,

        @Schema(description = "프로젝트 ID")
        Long projectId,

        @Schema(description = "테스트 그룹명")
        String groupName,

        @Schema(description = "타겟 레포지토리 ID")
        Long targetRepoId,

        @Schema(description = "타겟 브랜치명")
        String targetBranch
) {
    /**
     * TestGroup 엔티티를 DTO로 변환하는 정적 팩토리 메서드
     * 연관 관계가 있는 컬렉션(testExecutions)을 제외하여 무한 재귀를 방지합니다.
     */
    public static TestGroupResponseDto from(TestGroup group) {
        return TestGroupResponseDto.builder()
                .groupId(group.getGroupId())
                .projectId(group.getProjectId())
                .groupName(group.getGroupName())
                .targetRepoId(group.getTargetRepoId())
                .targetBranch(group.getTargetBranch())
                .build();
    }
}