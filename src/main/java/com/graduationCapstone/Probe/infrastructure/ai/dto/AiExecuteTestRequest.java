package com.graduationCapstone.Probe.infrastructure.ai.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public record AiExecuteTestRequest(

        @JsonProperty("execution_id")
        Long executionId,

        @JsonProperty("callback_url")
        String callbackUrl
) {
}