package com.graduationCapstone.Probe.domain.test.dto;

import java.time.LocalDateTime;

public record TestResultDetailFullDto(
        String tester, String status, LocalDateTime completedAt,
        String scenarioName, String description, String testCodeId, String testCaseName,
        String precondition, String testData, String executionSteps, String result
) {}
