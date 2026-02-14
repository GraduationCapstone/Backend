package com.graduationCapstone.Probe.domain.project.dto;

import java.util.List;

public record ProjectCreateRequestDto(
        String projectName,
        List<Long> repoIds
) {
}