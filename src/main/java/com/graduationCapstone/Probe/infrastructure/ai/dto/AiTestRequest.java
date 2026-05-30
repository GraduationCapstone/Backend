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
        Long executionId;

        @JsonProperty("repository_url")
        String repositoryUrl;

        @JsonProperty("target_branch")
        String targetBranch;

        @JsonProperty("base_url")
        String baseUrl;

        @JsonProperty("requirement")
        String requirement;

        @JsonProperty("auth_token")
        String authToken;

        @JsonProperty("callback_url")
        String callbackUrl;

        @JsonProperty("scenario_serial")
        String scenarioSerial;

        @JsonProperty("scenario_attempt")
        String scenarioAttempt;

        @JsonProperty("group_name")
        String groupName;

        @JsonProperty("tester_name")
        String testerName;
}