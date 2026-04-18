package com.graduationCapstone.Probe.domain.test.dto;

import java.time.LocalDateTime;

public record TestExecutionListDto(
        String testCaseId, String testCodeName, String status,
        String duration, String tester, LocalDateTime completedAt
) {}
