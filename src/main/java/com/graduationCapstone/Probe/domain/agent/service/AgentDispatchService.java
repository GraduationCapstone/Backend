package com.graduationCapstone.Probe.domain.agent.service;

import com.graduationCapstone.Probe.domain.project.entity.GithubRepository;
import com.graduationCapstone.Probe.domain.project.entity.Scenario;
import com.graduationCapstone.Probe.domain.project.entity.ScenarioGuide;
import com.graduationCapstone.Probe.domain.project.repository.GithubRepositoryRepository;
import com.graduationCapstone.Probe.domain.project.repository.ScenarioGuideRepository;
import com.graduationCapstone.Probe.domain.project.repository.ScenarioRepository;
import com.graduationCapstone.Probe.domain.test.entity.ExecutionStatus;
import com.graduationCapstone.Probe.domain.test.entity.TestExecution;
import com.graduationCapstone.Probe.domain.test.repository.TestExecutionRepository;
import com.graduationCapstone.Probe.global.exception.ErrorCode;
import com.graduationCapstone.Probe.global.exception.handler.CustomException;
import com.graduationCapstone.Probe.infrastructure.ai.dto.AiTestRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
public class AgentDispatchService {

    private final WebClient aiWebClient;
    private final TestExecutionRepository testExecutionRepository;
    private final GithubRepositoryRepository githubRepositoryRepository;
    private final ScenarioGuideRepository scenarioGuideRepository;
    private final ScenarioRepository scenarioRepository;

    @Value("${app.server.url}")
    private String serverUrl;

    private static final String AI_EXECUTE_PATH = "/api/generate-test";

    public AgentDispatchService(
            @Qualifier("aiWebClient") WebClient aiWebClient,
            TestExecutionRepository testExecutionRepository,
            GithubRepositoryRepository githubRepositoryRepository,
            ScenarioGuideRepository scenarioGuideRepository,
            ScenarioRepository scenarioRepository
    ) {
        this.aiWebClient = aiWebClient;
        this.testExecutionRepository = testExecutionRepository;
        this.githubRepositoryRepository = githubRepositoryRepository;
        this.scenarioGuideRepository = scenarioGuideRepository;
        this.scenarioRepository = scenarioRepository;
    }

    /**
     * [동기] 존재 확인 및 유효성 검증을 수행하고 AI 요청 객체를 구성한 뒤,
     * 비동기 AI 서버 호출을 위임합니다.
     * <p>
     * 이 메서드는 컨트롤러에서 동기적으로 호출되므로,
     * 검증 실패 시 CustomException이 그대로 전파되어 400/404 HTTP 응답이 반환됩니다.</p>
     *
     * @param executionId  테스트 실행 ID
     * @param scenarioId   시나리오 ID
     * @param targetBranch 테스트 대상 브랜치
     */
    public void validateAndPrepare(Long executionId, Long scenarioId, String targetBranch) {
        log.info("Validating dispatch request. ExecutionId={}, ScenarioId={}", executionId, scenarioId);

        // 1. 테스트 실행 정보 존재 확인 → 없으면 404
        TestExecution execution = testExecutionRepository.findById(executionId)
                .orElseThrow(() -> new CustomException(ErrorCode.RESOURCE_NOT_FOUND));

        log.info("Execution found. ProjectId={}", execution.getProjectId());

        // 2. 연결된 레포지토리 URL 조회 → 없으면 404
        GithubRepository targetRepo = githubRepositoryRepository.findByProjectId(execution.getProjectId())
                .orElseThrow(() -> new CustomException(ErrorCode.RESOURCE_NOT_FOUND));

        // 3. Scenario (base_url, auth_token) 조회 → 없으면 404
        Scenario scenario = scenarioRepository.findById(scenarioId)
                .orElseThrow(() -> new CustomException(ErrorCode.RESOURCE_NOT_FOUND));

        // 4. 시나리오 스텝 조회 → 비어 있으면 400
        List<ScenarioGuide> steps = scenarioGuideRepository.findAllByScenarioIdOrderByStepOrder(scenarioId);
        if (steps.isEmpty()) {
            throw new CustomException(ErrorCode.INVALID_ARGUMENT);
        }

        // 5. requirement 문자열 조합 (stepOrder 순으로 정렬된 testItem을 \n으로 연결)
        String requirement = steps.stream()
                .map(sg -> sg.getGuide().getTestItem())
                .collect(Collectors.joining("\n"));

        String callbackUrl = serverUrl + "/api/agent/callback";

        AiTestRequest request = new AiTestRequest(
                executionId,
                targetRepo.getRepoUrl(),
                targetBranch,
                requirement,
                scenario.getAuthToken(),
                callbackUrl
        );

        // 6. 검증 통과 → AI 서버 호출만 비동기로 위임
        dispatchToAgent(executionId, request);
    }

    /**
     * [비동기] 구성된 요청을 AI 서버로 전송합니다.
     *
     * <p>{@code validateAndPrepare()}에서 모든 검증이 완료된 후에만 호출되며,
     * HTTP 호출 자체만 별도 스레드에서 수행합니다.</p>
     */
    @Async("agentExecutor")
    public void dispatchToAgent(Long executionId, AiTestRequest request) {
        log.info("Dispatching to AI server. ExecutionId={}", executionId);

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
                                exec.updateStatus(ExecutionStatus.FAILED);
                                testExecutionRepository.save(exec);
                            });
                        }
                );
    }
}
