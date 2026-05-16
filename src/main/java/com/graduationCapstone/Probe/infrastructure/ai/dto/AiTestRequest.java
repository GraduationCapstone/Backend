package com.graduationCapstone.Probe.infrastructure.ai.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * AI 서버로 테스트 생성/실행을 요청할 때 사용하는 DTO.
 * record에서 일반 class로 변경하여 Jackson 매핑 안정성 확보
 */
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class AiTestRequest {

        @JsonProperty("execution_id")
        private Long executionId;

        @JsonProperty("repository_url")
        private String repositoryUrl;

        @JsonProperty("branch")
        private String branch;

        @JsonProperty("requirement")
        private String requirement;

        @JsonProperty("auth_token")
        private String authToken;

        @JsonProperty("callback_url")
        private String callbackUrl;

        @JsonProperty("scenario_serial")
        private String scenarioSerial;

        @JsonProperty("scenario_attempt")
        private String scenarioAttempt;
}