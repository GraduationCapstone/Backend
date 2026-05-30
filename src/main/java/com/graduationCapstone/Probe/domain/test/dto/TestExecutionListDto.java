package com.graduationCapstone.Probe.domain.test.dto;

import java.time.LocalDateTime;

public record TestExecutionListDto(
        Long executionId,
        Long groupId,
        String testCaseId,
        String testGroupName,
        String passRatio,
        String duration,
        String tester,
        String testerProfileImage,
        LocalDateTime completedAt
) {}
