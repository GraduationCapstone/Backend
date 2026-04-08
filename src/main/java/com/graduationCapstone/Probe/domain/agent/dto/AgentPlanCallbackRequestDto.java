package com.graduationCapstone.Probe.domain.agent.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotNull;

/**
 * 1단계 콜백: AI 서버가 테스트 계획서 생성 완료 후 POST하는 요청 DTO.
 * 계획서(plan.xlsx) S3 URL만 포함합니다.
 */
public record AgentPlanCallbackRequestDto(

        @NotNull(message = "실행 ID는 필수입니다.")
        @JsonProperty("execution_id")
        Long executionId,

        @JsonProperty("plan_s3_url")
        String planS3Url
) {
}
