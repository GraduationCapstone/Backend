package com.graduationCapstone.Probe.domain.agent.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.List;

/**
 * FastAPI AI 서버가 테스트 완료 후 메인서버로 POST하는 콜백 요청 DTO.
 */
public record AgentCallbackRequestDto(

        @NotNull(message = "실행 ID는 필수입니다.")
        @JsonProperty("execution_id")
        Long executionId,

        @NotBlank(message = "상태값은 필수입니다.")
        String status,             // COMPLETED | FAILED

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

            @NotBlank(message = "결과 상태값은 필수입니다.")
            String status,          // SUCCESS | FAIL

            @JsonProperty("error_log")
            String errorLog,

            @JsonProperty("screenshot_s3_url")
            String screenshotS3Url
    ) {}
}

