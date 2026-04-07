package com.graduationCapstone.Probe.domain.agent.dto;

import jakarta.validation.constraints.NotNull;

/**
 * 2단계 디스패치: 테스트 실행 트리거 요청 DTO.
 * PLAN_COMPLETED 상태의 TestExecution에 대해 테스트 시작을 요청합니다.
 */
public record AgentTestTriggerRequestDto(

        @NotNull(message = "실행 ID는 필수입니다.")
        Long executionId
) {
}
