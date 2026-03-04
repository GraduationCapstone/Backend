package com.graduationCapstone.Probe.infrastructure.ai.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

/**
 * FastAPI AI 서버로 테스트 실행을 요청할 때 사용하는 DTO.
 * FastAPI 스펙 확정 시 필드명(snake_case) 및 엔드포인트를 조정한다.
 */
public record AiTestRequest(

        @JsonProperty("execution_id")
        Long executionId,

        @JsonProperty("callback_url")
        String callbackUrl,

        @JsonProperty("target_repo_url")
        String targetRepoUrl,

        @JsonProperty("target_branch")
        String targetBranch,

        @JsonProperty("scenario_steps")
        List<ScenarioStepDto> scenarioSteps
) {
    public record ScenarioStepDto(
            @JsonProperty("step_order")
            int stepOrder,

            @JsonProperty("test_item")
            String testItem
    ) {}
}

