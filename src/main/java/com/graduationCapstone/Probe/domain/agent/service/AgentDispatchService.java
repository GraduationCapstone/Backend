package com.graduationCapstone.Probe.domain.agent.service;

import com.graduationCapstone.Probe.domain.project.entity.GithubRepository;
import com.graduationCapstone.Probe.domain.project.entity.Scenario;
import com.graduationCapstone.Probe.domain.project.entity.ScenarioGuide;
import com.graduationCapstone.Probe.domain.project.repository.GithubRepositoryRepository;
import com.graduationCapstone.Probe.domain.project.repository.ScenarioGuideRepository;
import com.graduationCapstone.Probe.domain.project.repository.ScenarioRepository;
import com.graduationCapstone.Probe.domain.test.entity.*;
import com.graduationCapstone.Probe.domain.test.repository.ScenarioSequenceRepository;
import com.graduationCapstone.Probe.domain.test.repository.TestExecutionRepository;
import com.graduationCapstone.Probe.domain.user.entity.User;
import com.graduationCapstone.Probe.global.exception.ErrorCode;
import com.graduationCapstone.Probe.global.exception.handler.CustomException;
import com.graduationCapstone.Probe.infrastructure.ai.dto.AiTestRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
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
    private final ScenarioSequenceRepository scenarioSequenceRepository;

    @Value("${app.server.url}")
    private String serverUrl;

    private static final String AI_PLAN_PATH = "/api/generate-plan";
    private static final String AI_TEST_PATH = "/api/execute-test";

    public AgentDispatchService(
            @Qualifier("aiWebClient") WebClient aiWebClient,
            TestExecutionRepository testExecutionRepository,
            GithubRepositoryRepository githubRepositoryRepository,
            ScenarioGuideRepository scenarioGuideRepository,
            ScenarioRepository scenarioRepository,
            ScenarioSequenceRepository scenarioSequenceRepository
    ) {
        this.aiWebClient = aiWebClient;
        this.testExecutionRepository = testExecutionRepository;
        this.githubRepositoryRepository = githubRepositoryRepository;
        this.scenarioGuideRepository = scenarioGuideRepository;
        this.scenarioRepository = scenarioRepository;
        this.scenarioSequenceRepository = scenarioSequenceRepository;
    }

    /**
     * 1단계: 테스트 계획서 생성 요청.
     *
     * <p>TestExecution을 생성하고 SCENARIO_SERIAL/ATTEMPT를 발급한 뒤,
     * AI 서버에 계획서 생성을 비동기로 요청합니다.</p>
     *
     * @param user         현재 인증된 사용자
     * @param projectId    프로젝트 ID
     * @param scenarioId   시나리오 ID
     * @param testItem     시나리오 가이드 이름 (예: "회원가입")
     * @param targetBranch 테스트 대상 브랜치
     * @return 생성된 TestExecution의 ID
     */
    @Transactional
    public Long dispatchPlan(User user, Long projectId, Long scenarioId,
                             String testItem, String targetBranch) {
        log.info("Dispatching plan. ProjectId={}, TestItem={}", projectId, testItem);

        // 1. 시나리오 시리얼 조회
        ScenarioSerial serial = ScenarioSerial.fromTestItem(testItem);

        // 2. SCENARIO_ATTEMPT 발급 (동시성 제어)
        ScenarioSequence sequence = scenarioSequenceRepository
                .findByProjectIdAndScenarioSerialForUpdate(projectId, serial.getCode())
                .orElseGet(() -> scenarioSequenceRepository.save(
                        ScenarioSequence.builder()
                                .projectId(projectId)
                                .scenarioSerial(serial.getCode())
                                .build()
                ));
        int attempt = sequence.nextAttempt();

        // 3. TestExecution 생성
        TestExecution execution = TestExecution.builder()
                .projectId(projectId)
                .tester(user)
                .testerName(user.getUsername())
                .scenarioSerial(serial.getCode())
                .scenarioAttempt(attempt)
                .testName(testItem)
                .status(ExecutionStatus.PLAN_GENERATING)
                .build();
        testExecutionRepository.save(execution);

        log.info("TestExecution created. Id={}, ScenarioId={}", execution.getExecutionId(), execution.getTestScenarioId());

        // 4. 레포지토리 URL 조회 -> projectId 대신 전달받은 repoId로 정확한 레포지토리 정보 조회
        GithubRepository targetRepo = githubRepositoryRepository.findByProjectId(projectId)
                .orElseThrow(() -> new CustomException(ErrorCode.RESOURCE_NOT_FOUND));

        // 5. Scenario (auth_token) 조회
        Scenario scenario = scenarioRepository.findById(scenarioId)
                .orElseThrow(() -> new CustomException(ErrorCode.RESOURCE_NOT_FOUND));

        // 6. 시나리오 스텝에서 requirement 구성
        List<ScenarioGuide> steps = scenarioGuideRepository.findAllByScenarioIdOrderByStepOrder(scenarioId);
        if (steps.isEmpty()) {
            throw new CustomException(ErrorCode.INVALID_ARGUMENT);
        }

        String requirement = steps.stream()
                .map(sg -> sg.getGuide().getTestItem())
                .collect(Collectors.joining("\n"));

        String callbackUrl = serverUrl + "/api/agent/callback/plan";

        AiTestRequest request = new AiTestRequest(
                execution.getExecutionId(),
                targetRepo.getRepoUrl(),
                targetBranch,
                requirement,
                scenario.getAuthToken(),
                callbackUrl,
                serial.getCode(),
                String.format("%02d", attempt)
        );

        // 7. AI 서버 호출 (비동기)
        dispatchToAi(execution.getExecutionId(), request, AI_PLAN_PATH);

        return execution.getExecutionId();
    }

    /**
     * 2단계: 테스트 실행 요청.
     *
     * <p>PLAN_COMPLETED 상태의 TestExecution에 대해 테스트 실행을 요청합니다.</p>
     *
     * @param executionId 테스트 실행 ID
     */
    @Transactional
    public void dispatchTest(Long executionId) {
        log.info("Dispatching test execution. ExecutionId={}", executionId);

        TestExecution execution = testExecutionRepository.findActiveById(executionId)
                .orElseThrow(() -> new CustomException(ErrorCode.RESOURCE_NOT_FOUND));

        if (execution.getStatus() != ExecutionStatus.PLAN_COMPLETED) {
            throw new CustomException(ErrorCode.INVALID_ARGUMENT);
        }

        // 상태 변경: TESTING (dirty checking으로 트랜잭션 커밋 시 자동 반영)
        execution.updateStatus(ExecutionStatus.TESTING);

        // 레포지토리 URL 조회
        GithubRepository targetRepo = githubRepositoryRepository.findByProjectId(execution.getProjectId())
                .orElseThrow(() -> new CustomException(ErrorCode.RESOURCE_NOT_FOUND));

        String callbackUrl = serverUrl + "/api/agent/callback/test";

        AiTestRequest request = new AiTestRequest(
                executionId,
                targetRepo.getRepoUrl(),
                null,   // branch는 이미 AI 서버가 알고 있음 (계획서 생성 시 전달됨)
                null,   // requirement도 동일
                null,   // authToken도 동일
                callbackUrl,
                execution.getScenarioSerial(),
                String.format("%02d", execution.getScenarioAttempt())
        );

        // AI 서버 호출 (비동기)
        dispatchToAi(executionId, request, AI_TEST_PATH);
    }

    /**
     * AI 서버로 요청을 비동기 전송합니다.
     */
    @Async("agentExecutor")
    public void dispatchToAi(Long executionId, AiTestRequest request, String path) {
        log.info("Dispatching to AI server. ExecutionId={}, Path={}", executionId, path);

        aiWebClient.post()
                .uri(path)
                .bodyValue(request)
                .retrieve()
                .toBodilessEntity()
                .subscribe(
                        response -> log.info("AI server dispatch success. ExecutionId={}, Status={}",
                                executionId, response.getStatusCode()),
                        error -> {
                            log.error("AI server dispatch failed. ExecutionId={}", executionId, error);
                            testExecutionRepository.findById(executionId).ifPresent(exec -> {
                                exec.updateStatus(ExecutionStatus.FAILED);
                                testExecutionRepository.save(exec);
                            });
                        }
                );
    }
}
