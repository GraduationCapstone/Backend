package com.graduationCapstone.Probe.domain.agent.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.graduationCapstone.Probe.domain.test.entity.ExecutionStatus;
import com.graduationCapstone.Probe.domain.test.entity.ResultStatus;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

import java.util.List;

/**
 * FastAPI AI 서버가 테스트 완료 후 메인서버로 POST하는 콜백 요청 DTO.
 */
public record AgentCallbackRequestDto(

        @NotNull(message = "실행 ID는 필수입니다.")
        @JsonProperty("execution_id")
        Long executionId,

        @NotNull(message = "상태값은 필수입니다.")
        ExecutionStatus status,     // COMPLETED | FAILED

        @JsonProperty("duration_ms")
        Long durationMs,

        @JsonProperty("report_s3_url")
        String reportS3Url,

        @Valid
        List<TestResultDto> results
) {
    public record TestResultDto(

            @JsonProperty("case_name")
            String caseName,

            @NotNull(message = "결과 상태값은 필수입니다.")
            ResultStatus status,    // SUCCESS | FAIL

            @JsonProperty("error_log")
            String errorLog,

            @JsonProperty("screenshot_s3_url")
            String screenshotS3Url
    ) {}
}

