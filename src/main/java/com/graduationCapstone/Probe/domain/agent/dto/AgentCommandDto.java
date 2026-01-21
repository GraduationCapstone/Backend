package com.graduationCapstone.Probe.domain.agent.dto;

import java.util.List;

public record AgentCommandDto(
        Long executionId,
        String targetRepoUrl,       // 테스트 대상 레포 주소
        String targetBranch,        // 테스트 대상 브랜치
        List<AgentScenarioStep> scenarioSteps // 수행할 시나리오 목록
) {
}
