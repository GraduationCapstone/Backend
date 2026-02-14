package com.graduationCapstone.Probe.domain.project.dto;

import java.util.List;

public record ProjectInviteRequestDto(
        List<String> usernames
) {
}
