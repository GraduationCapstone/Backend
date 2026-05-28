package com.graduationCapstone.Probe.domain.agent.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * 1단계 디스패치: 테스트 계획서 생성 트리거 요청 DTO.
 * 프론트엔드가 선택한 시나리오 가이드 이름(testItem)을 전달합니다.
 */
public record AgentPlanTriggerRequestDto(

        @NotNull(message = "프로젝트 ID는 필수입니다.")
        Long projectId,

        @NotNull(message = "시나리오 시리얼는 필수입니다.")
        String scenarioSerial,

        @NotBlank(message = "시나리오 가이드 이름은 필수입니다.")
        String testItem,        // 예: "회원가입", "로그인"

        @NotBlank(message = "타겟 브랜치는 필수입니다.")
        String targetBranch,

        @NotNull(message = "타겟 레포지토리 ID는 필수입니다.")
        Long targetRepoId,

        String optionalServerUrl,

        String groupName
) {
}
