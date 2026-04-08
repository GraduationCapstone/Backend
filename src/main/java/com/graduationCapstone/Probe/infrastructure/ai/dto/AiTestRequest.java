package com.graduationCapstone.Probe.infrastructure.ai.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * AI 서버로 테스트 생성/실행을 요청할 때 사용하는 DTO.
 */
public record AiTestRequest(

        @JsonProperty("execution_id")
        Long executionId,

        @JsonProperty("repository_url")
        String repositoryUrl,

        @JsonProperty("branch")
        String branch,

        @JsonProperty("requirement")
        String requirement,

        @JsonProperty("auth_token")
        String authToken,

        @JsonProperty("callback_url")
        String callbackUrl,

        @JsonProperty("scenario_serial")
        String scenarioSerial,

        @JsonProperty("scenario_attempt")
        String scenarioAttempt
) {
}
