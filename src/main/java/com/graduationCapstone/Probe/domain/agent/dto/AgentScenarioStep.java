package com.graduationCapstone.Probe.domain.agent.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public record AgentScenarioStep(
        int stepOrder,

        @JsonProperty("test_item")
        String templateText
) {
}
