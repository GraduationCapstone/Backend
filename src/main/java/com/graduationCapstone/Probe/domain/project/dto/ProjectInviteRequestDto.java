package com.graduationCapstone.Probe.domain.project.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;

import java.util.List;

@Schema(description = "프로젝트 팀원 초대 요청 DTO")
public record ProjectInviteRequestDto(
        @Schema(description = "초대할 팀원들의 이메일 리스트 (최소 1개, 최대 30개)", example = "[\"user@gmail.com\", \"user@naver.com\"]")
        @NotEmpty(message = "초대할 이메일 리스트가 비어있습니다.")
        @Size(min = 1, max = 20, message = "한 번에 1명에서 최대 20명까지만 초대할 수 있습니다.")
        List<@Email(message = "유효하지 않은 이메일 형식이 포함되어 있습니다.") String> emails
) {}
