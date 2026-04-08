package com.graduationCapstone.Probe.domain.agent.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.graduationCapstone.Probe.domain.test.entity.ExecutionStatus;
import com.graduationCapstone.Probe.domain.test.entity.ResultStatus;
import com.graduationCapstone.Probe.domain.test.entity.ScenarioDetailData;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

import java.util.List;

/**
 * 2단계 콜백: AI 서버가 테스트 실행 완료 후 POST하는 요청 DTO.
 * 테스트 결과, 산출물 URL 전체가 포함됩니다.
 */
public record AgentTestCallbackRequestDto(

        @NotNull(message = "실행 ID는 필수입니다.")
        @JsonProperty("execution_id")
        Long executionId,

        @NotNull(message = "상태값은 필수입니다.")
        ExecutionStatus status,     // COMPLETED | FAILED

        @JsonProperty("duration_seconds")
        Double durationSeconds,

        @JsonProperty("plan_result_s3_url")
        String planResultS3Url,     // plan_result.xlsx

        @JsonProperty("test_spec_s3_url")
        String testSpecS3Url,       // test.spec.js

        @Valid
        List<TestResultDto> results
) {
    public record TestResultDto(

            @JsonProperty("test_case_number")
            String testCaseNumber,      // AI가 부여한 2자리 번호

            @JsonProperty("case_name")
            String caseName,

            @JsonProperty("test_code_name")
            String testCodeName,

            @NotNull(message = "결과 상태값은 필수입니다.")
            ResultStatus status,        // PASS | BLOCK | FAIL | UNTEST

            @JsonProperty("duration_seconds")
            Double durationSeconds,

            @JsonProperty("error_log")
            String errorLog,

            @JsonProperty("test_code")
            String testCode,

            @JsonProperty("scenario_detail")
            ScenarioDetailData scenarioDetail,

            @JsonProperty("screenshot_s3_urls")
            List<String> screenshotS3Urls
    ) {}
}
