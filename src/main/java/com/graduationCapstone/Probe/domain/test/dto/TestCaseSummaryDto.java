package com.graduationCapstone.Probe.domain.test.dto;

import java.time.LocalDateTime;

public record TestCaseSummaryDto(
        String testCaseId, String testGroupName,
        String passRatio, String duration, String tester, LocalDateTime completedAt
) {}
