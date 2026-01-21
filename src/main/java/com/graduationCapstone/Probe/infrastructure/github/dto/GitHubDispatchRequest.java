package com.graduationCapstone.Probe.infrastructure.github.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.graduationCapstone.Probe.domain.agent.dto.AgentCommandDto;

public record GitHubDispatchRequest(
        @JsonProperty("event_type")
        String eventType,

        @JsonProperty("client_payload")
        AgentCommandDto clientPayload
) {
}
