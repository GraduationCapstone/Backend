package com.graduationCapstone.Probe.domain.test.dto;

import java.time.LocalDateTime;

public record TestResultDetailBasicDto(String tester, String status, LocalDateTime completedAt, String errorLog) {}
