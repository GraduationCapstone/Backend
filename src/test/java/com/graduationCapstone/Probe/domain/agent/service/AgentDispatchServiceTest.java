package com.graduationCapstone.Probe.domain.agent.service;

import com.graduationCapstone.Probe.domain.project.entity.GithubRepository;
import com.graduationCapstone.Probe.domain.project.entity.Guide;
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
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.reactive.function.client.WebClient;

import java.io.IOException;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
class AgentDispatchServiceTest {

    private AgentDispatchService agentDispatchService;
    private MockWebServer mockWebServer;

    @Mock
    private TestExecutionRepository testExecutionRepository;
    @Mock
    private GithubRepositoryRepository githubRepositoryRepository;
    @Mock
    private ScenarioGuideRepository scenarioGuideRepository;
    @Mock
    private ScenarioRepository scenarioRepository;

    @BeforeEach
    void setUp() throws IOException {
        mockWebServer = new MockWebServer();
        mockWebServer.start();

        String baseUrl = mockWebServer.url("/").toString();
        WebClient realWebClient = WebClient.builder()
                .baseUrl(baseUrl)
                .build();

        agentDispatchService = new AgentDispatchService(
                realWebClient,
                testExecutionRepository,
                githubRepositoryRepository,
                scenarioGuideRepository,
                scenarioRepository
        );

        ReflectionTestUtils.setField(agentDispatchService, "serverUrl",
                mockWebServer.url("").toString().replaceAll("/$", ""));
    }

    @AfterEach
    void tearDown() throws IOException {
        mockWebServer.shutdown();
    }

    // -------------------------------------------------------
    // 1. 정상 dispatch — AI 서버로 올바른 JSON 전송
    // -------------------------------------------------------
    @Test
    @DisplayName("정상적인 Dispatch 요청 시 AI 서버 규격에 맞는 JSON이 전송됨")
    void validateAndPrepare_success() throws InterruptedException {
        // given
        Long executionId = 1L;
        Long scenarioId = 1L;
        Long projectId = 100L;
        String targetBranch = "feature/login";

        TestExecution mockExecution = TestExecution.builder()
                .executionId(executionId)
                .projectId(projectId)
                .build();
        given(testExecutionRepository.findById(executionId)).willReturn(Optional.of(mockExecution));

        GithubRepository mockRepo = GithubRepository.builder()
                .repoUrl("https://github.com/school/target-repo.git")
                .build();
        given(githubRepositoryRepository.findByProjectId(projectId)).willReturn(Optional.of(mockRepo));

        Scenario mockScenario = Scenario.builder()
                .scenarioId(scenarioId)
                .authToken("Bearer test-token-123")
                .build();
        given(scenarioRepository.findById(scenarioId)).willReturn(Optional.of(mockScenario));

        Guide mockGuide = Guide.builder()
                .testItem("로그인 버튼을 누른다")
                .build();
        ScenarioGuide mockScenarioGuide = ScenarioGuide.builder()
                .stepOrder(1)
                .guide(mockGuide)
                .build();
        given(scenarioGuideRepository.findAllByScenarioIdOrderByStepOrder(scenarioId))
                .willReturn(List.of(mockScenarioGuide));

        mockWebServer.enqueue(new MockResponse().setResponseCode(202));

        // when
        agentDispatchService.validateAndPrepare(executionId, scenarioId, targetBranch);

        // then
        RecordedRequest request = mockWebServer.takeRequest(5, TimeUnit.SECONDS);

        assertThat(request).isNotNull();

        // 1. URL 및 메서드 확인
        assertThat(request.getMethod()).isEqualTo("POST");
        assertThat(request.getPath()).isEqualTo("/api/generate-test");

        // 2. Body 내용 확인
        String body = request.getBody().readUtf8();
        assertThat(body).contains("\"execution_id\":1");
        assertThat(body).contains("\"callback_url\":");
        assertThat(body).contains("/api/agent/callback");
        assertThat(body).contains("\"repository_url\":\"https://github.com/school/target-repo.git\"");
        assertThat(body).contains("\"branch\":\"feature/login\"");
        assertThat(body).contains("\"requirement\":\"로그인 버튼을 누른다\"");
        assertThat(body).contains("\"auth_token\":\"Bearer test-token-123\"");

        // 3. 제거된 필드가 없는지 확인
        assertThat(body).doesNotContain("scenario_steps");
        assertThat(body).doesNotContain("step_order");
        assertThat(body).doesNotContain("target_repo_url");
        assertThat(body).doesNotContain("target_branch");
    }

    // -------------------------------------------------------
    // 2. [동기 구간] executionId 없음 → 즉시 404 예외
    // -------------------------------------------------------
    @Test
    @DisplayName("존재하지 않는 executionId → RESOURCE_NOT_FOUND 예외가 동기적으로 발생")
    void validateAndPrepare_executionNotFound_throwsImmediately() {
        // given
        Long nonExistentId = 999L;
        given(testExecutionRepository.findById(nonExistentId)).willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> agentDispatchService.validateAndPrepare(nonExistentId, 1L, "main"))
                .isInstanceOf(CustomException.class)
                .extracting(e -> ((CustomException) e).getErrorCode())
                .isEqualTo(ErrorCode.RESOURCE_NOT_FOUND);
    }

    // -------------------------------------------------------
    // 3. [동기 구간] scenarioId 없음 → 즉시 404 예외
    // -------------------------------------------------------
    @Test
    @DisplayName("존재하지 않는 scenarioId → RESOURCE_NOT_FOUND 예외가 동기적으로 발생")
    void validateAndPrepare_scenarioNotFound_throwsImmediately() {
        // given
        Long executionId = 1L;
        Long projectId = 100L;

        TestExecution mockExecution = TestExecution.builder()
                .executionId(executionId)
                .projectId(projectId)
                .build();
        given(testExecutionRepository.findById(executionId)).willReturn(Optional.of(mockExecution));

        GithubRepository mockRepo = GithubRepository.builder()
                .repoUrl("https://github.com/school/target-repo.git")
                .build();
        given(githubRepositoryRepository.findByProjectId(projectId)).willReturn(Optional.of(mockRepo));

        given(scenarioRepository.findById(1L)).willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> agentDispatchService.validateAndPrepare(executionId, 1L, "main"))
                .isInstanceOf(CustomException.class)
                .extracting(e -> ((CustomException) e).getErrorCode())
                .isEqualTo(ErrorCode.RESOURCE_NOT_FOUND);
    }

    // -------------------------------------------------------
    // 4. [동기 구간] 시나리오 스텝 없음 → 즉시 400 예외
    // -------------------------------------------------------
    @Test
    @DisplayName("시나리오 스텝이 없을 때 → INVALID_ARGUMENT 예외가 동기적으로 발생")
    void validateAndPrepare_emptyScenarioSteps_throwsImmediately() {
        // given
        Long executionId = 1L;
        Long projectId = 100L;

        TestExecution mockExecution = TestExecution.builder()
                .executionId(executionId)
                .projectId(projectId)
                .build();
        given(testExecutionRepository.findById(executionId)).willReturn(Optional.of(mockExecution));

        GithubRepository mockRepo = GithubRepository.builder()
                .repoUrl("https://github.com/school/target-repo.git")
                .build();
        given(githubRepositoryRepository.findByProjectId(projectId)).willReturn(Optional.of(mockRepo));

        Scenario mockScenario = Scenario.builder()
                .scenarioId(1L)
                .authToken(null)
                .build();
        given(scenarioRepository.findById(1L)).willReturn(Optional.of(mockScenario));

        given(scenarioGuideRepository.findAllByScenarioIdOrderByStepOrder(1L))
                .willReturn(List.of());

        // when & then
        assertThatThrownBy(() -> agentDispatchService.validateAndPrepare(executionId, 1L, "main"))
                .isInstanceOf(CustomException.class)
                .extracting(e -> ((CustomException) e).getErrorCode())
                .isEqualTo(ErrorCode.INVALID_ARGUMENT);
    }

    // -------------------------------------------------------
    // 5. [비동기 구간] AI 서버 500 에러 → TestExecution FAILED 업데이트
    // -------------------------------------------------------
    @Test
    @DisplayName("AI 서버 호출 실패 시 TestExecution 상태가 FAILED로 업데이트됨")
    void validateAndPrepare_aiServerError_updatesStatusToFailed() throws InterruptedException {
        // given
        Long executionId = 2L;
        Long scenarioId = 1L;
        Long projectId = 200L;
        String targetBranch = "main";

        TestExecution mockExecution = TestExecution.builder()
                .executionId(executionId)
                .projectId(projectId)
                .status(ExecutionStatus.PENDING)
                .build();
        given(testExecutionRepository.findById(executionId)).willReturn(Optional.of(mockExecution));

        GithubRepository mockRepo = GithubRepository.builder()
                .repoUrl("https://github.com/school/target-repo.git")
                .build();
        given(githubRepositoryRepository.findByProjectId(projectId)).willReturn(Optional.of(mockRepo));

        Scenario mockScenario = Scenario.builder()
                .scenarioId(scenarioId)
                .authToken(null)
                .build();
        given(scenarioRepository.findById(scenarioId)).willReturn(Optional.of(mockScenario));

        Guide mockGuide = Guide.builder().testItem("회원가입 버튼 클릭").build();
        ScenarioGuide mockScenarioGuide = ScenarioGuide.builder().stepOrder(1).guide(mockGuide).build();
        given(scenarioGuideRepository.findAllByScenarioIdOrderByStepOrder(scenarioId))
                .willReturn(List.of(mockScenarioGuide));

        mockWebServer.enqueue(new MockResponse().setResponseCode(500).setBody("Internal Server Error"));

        // when
        agentDispatchService.validateAndPrepare(executionId, scenarioId, targetBranch);

        mockWebServer.takeRequest(5, TimeUnit.SECONDS);
        Thread.sleep(500);

        // then
        org.mockito.Mockito.verify(testExecutionRepository, org.mockito.Mockito.atLeastOnce())
                .save(mockExecution);
    }
}
