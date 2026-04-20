package com.graduationCapstone.Probe.domain.test.dto;

import jakarta.validation.constraints.NotBlank;

public record TestGroupNameUpdateDto(
        @NotBlank(message = "수정할 그룹명을 입력해주세요.")
        String newGroupName
) {}
