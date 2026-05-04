package com.graduationCapstone.Probe.domain.test.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.util.List;

public record TestGroupCreateRequestDto(
        @NotBlank(message = "테스트 그룹명은 필수입니다.")
        String baseTestGroupName,

        @NotNull(message = "레포지토리 ID는 필수입니다.")
        Long targetRepoId,

        @NotEmpty(message = "최소 하나 이상의 시나리오를 선택해야 합니다.")
        List<String> scenarioSerials,

        @NotBlank(message = "대상 브랜치는 필수입니다.")
        String targetBranch,

        String optionalServerUrl
) {}