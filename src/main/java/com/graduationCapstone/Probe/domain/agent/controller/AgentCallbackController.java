package com.graduationCapstone.Probe.domain.agent.controller;

import com.graduationCapstone.Probe.domain.agent.dto.AgentCallbackRequestDto;
import com.graduationCapstone.Probe.domain.agent.service.AgentCallbackService;
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
        description = "AI 테스트 에이전트 제어 및 결과 수신"
)
@RestController
@RequestMapping("/api/agent")
@RequiredArgsConstructor
public class AgentCallbackController {

    private final AgentCallbackService agentCallbackService;

    @Operation(
            summary = "테스트 결과 콜백 수신",
            description = "FastAPI AI 서버가 테스트 완료 후 결과를 POST합니다. JWT 인증 없이 VPC 내부에서만 호출됩니다."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "결과 처리 완료"),
            @ApiResponse(responseCode = "400", description = "잘못된 요청 인자"),
            @ApiResponse(responseCode = "404", description = "해당 실행 정보를 찾을 수 없음")
    })
    @PostMapping("/callback")
    public ResponseEntity<Void> receiveCallback(
            @Valid
            @RequestBody
            AgentCallbackRequestDto requestDto
    ) {
        agentCallbackService.handleCallback(requestDto);
        return ResponseEntity.ok().build();
    }
}


