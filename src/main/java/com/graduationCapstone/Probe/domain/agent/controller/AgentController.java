package com.graduationCapstone.Probe.domain.agent.controller;

import com.graduationCapstone.Probe.domain.agent.dto.AgentPlanTriggerRequestDto;
import com.graduationCapstone.Probe.domain.agent.dto.AgentTestTriggerRequestDto;
import com.graduationCapstone.Probe.domain.agent.service.AgentDispatchService;
import com.graduationCapstone.Probe.domain.user.entity.User;
import io.swagger.v3.oas.annotations.Hidden;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@Tag(
        name = "AI 에이전트 (Agent)",
        description = "AI 테스트 에이전트 제어 및 결과 수신"
)
@RestController
@RequestMapping("/api/agent")
@RequiredArgsConstructor
public class AgentController {

    private final AgentDispatchService agentDispatchService;

    @Operation(
            summary = "1단계: 테스트 계획서 생성 트리거",
            description = "AI 서버에 테스트 계획서 생성을 비동기로 요청합니다. " +
                    "TestExecution을 생성하고 SCENARIO_SERIAL/ATTEMPT를 발급합니다."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "202", description = "계획서 생성 요청 접수됨"),
            @ApiResponse(responseCode = "400", description = "잘못된 요청 인자"),
            @ApiResponse(responseCode = "404", description = "프로젝트 또는 시나리오를 찾을 수 없음")
    })
    @Hidden
    @PostMapping("/dispatch/plan")
    public ResponseEntity<Map<String, Long>> triggerPlanGeneration(
            @AuthenticationPrincipal User user,
            @Valid @RequestBody AgentPlanTriggerRequestDto requestDto
    ) {
        Long executionId = agentDispatchService.dispatchPlan(
                user,
                requestDto.projectId(),
                requestDto.scenarioSerial(),
                requestDto.testItem(),
                requestDto.targetBranch(),
                requestDto.targetRepoId(),
                requestDto.optionalServerUrl()
        );

        return ResponseEntity.accepted().body(Map.of("executionId", executionId));
    }

    @Operation(
            summary = "2단계: 테스트 실행 트리거",
            description = "PLAN_COMPLETED 상태의 TestExecution에 대해 테스트 실행을 비동기로 요청합니다."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "202", description = "테스트 실행 요청 접수됨"),
            @ApiResponse(responseCode = "400", description = "잘못된 요청 인자 또는 계획서 미완료 상태"),
            @ApiResponse(responseCode = "404", description = "해당 실행 정보를 찾을 수 없음")
    })
    @PostMapping("/dispatch/test")
    public ResponseEntity<Void> triggerTestExecution(
            @Valid @RequestBody AgentTestTriggerRequestDto requestDto
    ) {
        agentDispatchService.dispatchTest(requestDto.executionId());
        return ResponseEntity.accepted().build();
    }
}
