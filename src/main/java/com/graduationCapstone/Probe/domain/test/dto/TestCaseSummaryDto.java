package com.graduationCapstone.Probe.domain.test.dto;

import java.time.LocalDateTime;

public record TestCaseSummaryDto(
        Long resultId,
        String testCaseId,
        String testCaseName,
        String status,
        String duration,
        String tester,
        String testerProfileImage,
        LocalDateTime completedAt
) {}
