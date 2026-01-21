package com.graduationCapstone.Probe.domain.agent.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record AgentTriggerRequestDto(
        @NotNull(message = "실행 ID는 필수입니다.")
        Long executionId,

        @NotNull(message = "시나리오 ID는 필수입니다.")
        Long scenarioId,

        @NotBlank(message = "타겟 브랜치는 필수입니다.")
        String targetBranch
) {
}
