package com.graduationCapstone.Probe.domain.test.dto;

import jakarta.validation.constraints.NotBlank;

public record TestCodeNameUpdateDto(
        @NotBlank(message = "수정할 테스트 코드명을 입력해주세요.")
        String newTestCodeName
) {}
