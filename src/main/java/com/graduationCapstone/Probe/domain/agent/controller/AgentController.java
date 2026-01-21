package com.graduationCapstone.Probe.domain.agent.controller;

import com.graduationCapstone.Probe.domain.agent.dto.AgentTriggerRequestDto;
import com.graduationCapstone.Probe.domain.agent.service.AgentDispatchService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(
        name = "AI 에이전트 (Agent)",
        description = "GitHub Actions 기반의 AI 테스트 에이전트 제어"
)
@RestController
@RequestMapping("/api/agent")
@RequiredArgsConstructor
public class AgentController {

    private final AgentDispatchService agentDispatchService;

    @Operation(
            summary = "테스트 실행 트리거 (Dispatch)",
            description = "GitHub Actions Runner에게 테스트 시작 명령을 비동기로 전송합니다."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "202", description = "명령 전송 요청 접수됨 (비동기 처리 시작)"),
            @ApiResponse(responseCode = "400", description = "잘못된 요청 인자"),
            @ApiResponse(responseCode = "404", description = "해당 실행 정보나 레포지토리를 찾을 수 없음")
    })
    @PostMapping("/dispatch")
    public ResponseEntity<Void> triggerAgent(
            @Valid
            @RequestBody
            AgentTriggerRequestDto requestDto
    ) {

        agentDispatchService.triggerAgentExecution(
                requestDto.executionId(),
                requestDto.scenarioId(),
                requestDto.targetBranch()
        );

        // 비동기 요청이므로 Accepted(202) 상태 코드를 반환
        return ResponseEntity.accepted().build();
    }
}
