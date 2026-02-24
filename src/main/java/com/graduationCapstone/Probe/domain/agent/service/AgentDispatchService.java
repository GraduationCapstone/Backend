package com.graduationCapstone.Probe.domain.agent.service;

import com.graduationCapstone.Probe.domain.project.entity.GithubRepository;
import com.graduationCapstone.Probe.domain.project.entity.ScenarioGuide;
import com.graduationCapstone.Probe.domain.project.repository.GithubRepositoryRepository;
import com.graduationCapstone.Probe.domain.project.repository.ScenarioGuideRepository;
import com.graduationCapstone.Probe.domain.test.entity.TestExecution;
import com.graduationCapstone.Probe.domain.test.repository.TestExecutionRepository;
import com.graduationCapstone.Probe.global.exception.ErrorCode;
import com.graduationCapstone.Probe.global.exception.handler.CustomException;
import com.graduationCapstone.Probe.infrastructure.ai.dto.AiTestRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.List;

@Slf4j
@Service
public class AgentDispatchService {

    private final WebClient aiWebClient;
    private final TestExecutionRepository testExecutionRepository;
    private final GithubRepositoryRepository githubRepositoryRepository;
    private final ScenarioGuideRepository scenarioGuideRepository;

    @Value("${app.server.url}")
    private String serverUrl;

    // FastAPI 엔드포인트 — 스펙 확정 시 수정
    private static final String AI_EXECUTE_PATH = "/api/v1/test/execute";

    public AgentDispatchService(
            @Qualifier("aiWebClient") WebClient aiWebClient,
            TestExecutionRepository testExecutionRepository,
            GithubRepositoryRepository githubRepositoryRepository,
            ScenarioGuideRepository scenarioGuideRepository
    ) {
        this.aiWebClient = aiWebClient;
        this.testExecutionRepository = testExecutionRepository;
        this.githubRepositoryRepository = githubRepositoryRepository;
        this.scenarioGuideRepository = scenarioGuideRepository;
    }

    @Async
    public void triggerAgentExecution(Long executionId, Long scenarioId, String targetBranch) {
        log.info("Starting Agent Dispatch for Execution ID: {}", executionId);

        // 테스트 실행 정보 조회
        TestExecution execution = testExecutionRepository.findById(executionId)
                .orElseThrow(() -> new CustomException(ErrorCode.RESOURCE_NOT_FOUND));

        log.info("Project ID from Execution: {}", execution.getProjectId());

        // 타겟 레포지토리 URL 조회
        GithubRepository targetRepo = githubRepositoryRepository.findByProjectId(execution.getProjectId())
                .orElseThrow(() -> new CustomException(ErrorCode.RESOURCE_NOT_FOUND));

        // 시나리오 스텝 조회
        List<ScenarioGuide> steps = scenarioGuideRepository.findAllByScenarioIdOrderByStepOrder(scenarioId);

        if (steps.isEmpty()) {
            throw new CustomException(ErrorCode.INVALID_ARGUMENT);
        }

        List<AiTestRequest.ScenarioStepDto> stepDtos = steps.stream()
                .map(sg -> new AiTestRequest.ScenarioStepDto(
                        sg.getStepOrder(),
                        sg.getGuide().getTestItem()
                ))
                .toList();

        String callbackUrl = serverUrl + "/api/agent/callback";

        AiTestRequest request = new AiTestRequest(
                executionId,
                callbackUrl,
                targetRepo.getRepoUrl(),
                targetBranch,
                stepDtos
        );

        // AI 서버로 테스트 실행 요청 전송 (비동기)
        aiWebClient.post()
                .uri(AI_EXECUTE_PATH)
                .bodyValue(request)
                .retrieve()
                .toBodilessEntity()
                .subscribe(
                        response -> log.info("AI server dispatch success. ExecutionId={}, Status={}",
                                executionId, response.getStatusCode()),
                        error -> {
                            log.error("AI server dispatch failed. ExecutionId={}", executionId, error);
                            // 호출 실패 시 TestExecution 상태를 FAILED로 업데이트
                            testExecutionRepository.findById(executionId).ifPresent(exec -> {
                                exec.updateStatus("FAILED");
                                testExecutionRepository.save(exec);
                            });
                        }
                );
    }
}
