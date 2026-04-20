package com.graduationCapstone.Probe.domain.test.dto;

import java.time.LocalDateTime;

public record TestResultDetailCodeDto(String tester, String status, LocalDateTime completedAt, String testCode) {}
