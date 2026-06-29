package com.graduationCapstone.Probe.domain.agent.service;

import com.graduationCapstone.Probe.domain.project.entity.GithubRepository;
import com.graduationCapstone.Probe.domain.project.entity.Guide;
import com.graduationCapstone.Probe.domain.project.entity.Scenario;
import com.graduationCapstone.Probe.domain.project.entity.ScenarioGuide;
import com.graduationCapstone.Probe.domain.project.repository.GithubRepositoryRepository;
import com.graduationCapstone.Probe.domain.project.repository.ScenarioGuideRepository;
import com.graduationCapstone.Probe.domain.project.repository.ScenarioRepository;
import com.graduationCapstone.Probe.domain.test.entity.ExecutionStatus;
import com.graduationCapstone.Probe.domain.test.entity.ScenarioSequence;
import com.graduationCapstone.Probe.domain.test.entity.TestExecution;
import com.graduationCapstone.Probe.domain.test.repository.ScenarioSequenceRepository;
import com.graduationCapstone.Probe.domain.test.repository.TestExecutionRepository;
import com.graduationCapstone.Probe.domain.user.entity.User;
import com.graduationCapstone.Probe.global.exception.ErrorCode;
import com.graduationCapstone.Probe.global.exception.handler.CustomException;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

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
    @Mock
    private ScenarioSequenceRepository scenarioSequenceRepository;

    private User testUser;

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
                scenarioRepository,
                scenarioSequenceRepository
        );

        ReflectionTestUtils.setField(agentDispatchService, "serverUrl",
                mockWebServer.url("").toString().replaceAll("/$", ""));

        testUser = User.builder()
                .id(1L)
                .githubId("12345")
                .username("tester")
                .email("tester@test.com")
                .build();
    }

    @AfterEach
    void tearDown() throws IOException {
        mockWebServer.shutdown();
    }

    // =======================================================
    // 1단계: dispatchPlan
    // =======================================================
    @Nested
    @DisplayName("1단계 - dispatchPlan")
    class DispatchPlanTest {

        @Test
        @DisplayName("정상 계획서 생성 요청 시 TestExecution이 생성되고 AI 서버에 올바른 JSON 전송")
        void dispatchPlan_success() throws InterruptedException {
            // given
            Long projectId = 100L;
            String scenarioSerial = "01";
            String testItem = "회원가입";
            String targetBranch = "feature/login";
            Long targetRepoId = 2L;
            String baseUrl = "http://localhost:8080";
            String executionName = "TestGroup (회원가입)";

            // ScenarioSequence가 없는 경우 → 새로 생성
            given(scenarioSequenceRepository.findByProjectIdAndScenarioSerialForUpdate(projectId, "01"))
                    .willReturn(Optional.empty());

            ScenarioSequence newSequence = ScenarioSequence.builder()
                    .projectId(projectId)
                    .scenarioSerial("01")
                    .build();
            given(scenarioSequenceRepository.save(any(ScenarioSequence.class))).willReturn(newSequence);

            // TestExecution 저장 시 executionId 반환
            given(testExecutionRepository.save(any(TestExecution.class))).willAnswer(invocation -> {
                TestExecution te = invocation.getArgument(0);
                ReflectionTestUtils.setField(te, "executionId", 1L);
                return te;
            });

            GithubRepository mockRepo = GithubRepository.builder()
                    .repoUrl("https://github.com/school/target-repo.git")
                    .build();
            given(githubRepositoryRepository.findByProjectIdAndGithubRepoId(projectId, targetRepoId))
                    .willReturn(Optional.of(mockRepo));

            Scenario mockScenario = Scenario.builder()
                    .scenarioId(1L)
                    .authToken("Bearer test-token-123")
                    .build();
            given(scenarioRepository.findByProjectIdAndScenarioSerial(projectId, scenarioSerial)).willReturn(Optional.of(mockScenario));

            Guide mockGuide = Guide.builder().testItem("회원가입 버튼을 누른다").build();
            ScenarioGuide mockScenarioGuide = ScenarioGuide.builder()
                    .stepOrder(1)
                    .guide(mockGuide)
                    .build();
            given(scenarioGuideRepository.findAllByScenarioIdOrderByStepOrder(1L))
                    .willReturn(List.of(mockScenarioGuide));

            mockWebServer.enqueue(new MockResponse().setResponseCode(202));

            // when
            Long executionId = agentDispatchService.dispatchPlan(
                    testUser, projectId, scenarioSerial, testItem, targetBranch, targetRepoId, baseUrl, executionName);

            // then — TestExecution 생성 확인
            ArgumentCaptor<TestExecution> executionCaptor = ArgumentCaptor.forClass(TestExecution.class);
            verify(testExecutionRepository).save(executionCaptor.capture());
            TestExecution savedExecution = executionCaptor.getValue();

            assertThat(executionId).isEqualTo(1L);
            assertThat(savedExecution.getProjectId()).isEqualTo(projectId);
            assertThat(savedExecution.getTester()).isEqualTo(testUser);
            assertThat(savedExecution.getTesterName()).isEqualTo("tester");
            assertThat(savedExecution.getScenarioSerial()).isEqualTo("01");
            assertThat(savedExecution.getScenarioAttempt()).isEqualTo(1);
            assertThat(savedExecution.getTestName()).isEqualTo("회원가입");
            assertThat(savedExecution.getStatus()).isEqualTo(ExecutionStatus.PLAN_GENERATING);

            // AI 서버 요청 확인
            RecordedRequest request = mockWebServer.takeRequest(5, TimeUnit.SECONDS);
            assertThat(request).isNotNull();
            assertThat(request.getMethod()).isEqualTo("POST");
            assertThat(request.getPath()).isEqualTo("/api/generate-plan");

            String body = request.getBody().readUtf8();
            assertThat(body).contains("\"execution_id\":1");
            assertThat(body).contains("\"repository_url\":\"https://github.com/school/target-repo.git\"");
            assertThat(body).contains("\"target_branch\":\"feature/login\"");
            assertThat(body).contains("http://localhost:8080");
            assertThat(body).contains("\"requirement\":\"회원가입 버튼을 누른다\"");
            assertThat(body).contains("\"auth_token\":\"Bearer test-token-123\"");
            assertThat(body).contains("/api/agent/callback/plan");
            assertThat(body).contains("\"scenario_serial\":\"01\"");
            assertThat(body).contains("\"scenario_attempt\":\"01\"");
            assertThat(body).contains("TestGroup (회원가입)");
            assertThat(body).contains("tester");
        }

        @Test
        @DisplayName("기존 ScenarioSequence가 있으면 attempt가 누적 증가함")
        void dispatchPlan_existingSequence_incrementsAttempt() {
            // given
            Long projectId = 100L;
            String scenarioSerial = "01";
            Long targetRepoId = 2L;
            String baseUrl = "http://localhost:8080";
            String executionName = "TestGroup (회원가입)";

            ScenarioSequence existingSequence = ScenarioSequence.builder()
                    .projectId(projectId)
                    .scenarioSerial("01")
                    .lastAttempt(3)
                    .build();
            given(scenarioSequenceRepository.findByProjectIdAndScenarioSerialForUpdate(projectId, "01"))
                    .willReturn(Optional.of(existingSequence));

            given(testExecutionRepository.save(any(TestExecution.class))).willAnswer(invocation -> {
                TestExecution te = invocation.getArgument(0);
                ReflectionTestUtils.setField(te, "executionId", 1L);
                return te;
            });

            GithubRepository mockRepo = GithubRepository.builder()
                    .repoUrl("https://github.com/school/repo.git")
                    .build();
            given(githubRepositoryRepository.findByProjectIdAndGithubRepoId(projectId,targetRepoId)).willReturn(Optional.of(mockRepo));

            Scenario mockScenario = Scenario.builder().scenarioId(1L).build();
            given(scenarioRepository.findByProjectIdAndScenarioSerial(projectId, scenarioSerial)).willReturn(Optional.of(mockScenario));

            Guide mockGuide = Guide.builder().testItem("회원가입").build();
            ScenarioGuide sg = ScenarioGuide.builder().stepOrder(1).guide(mockGuide).build();
            given(scenarioGuideRepository.findAllByScenarioIdOrderByStepOrder(1L))
                    .willReturn(List.of(sg));

            mockWebServer.enqueue(new MockResponse().setResponseCode(202));

            // when
            agentDispatchService.dispatchPlan(testUser, projectId, scenarioSerial, "회원가입", "main", targetRepoId, baseUrl, executionName);

            // then — attempt가 4로 증가
            ArgumentCaptor<TestExecution> captor = ArgumentCaptor.forClass(TestExecution.class);
            verify(testExecutionRepository).save(captor.capture());
            assertThat(captor.getValue().getScenarioAttempt()).isEqualTo(4);
            assertThat(captor.getValue().getTestScenarioId()).isEqualTo("T0104");
        }

        @Test
        @DisplayName("알 수 없는 testItem이면 IllegalArgumentException 발생")
        void dispatchPlan_unknownTestItem_throwsException() {
            Scenario mockScenario = Scenario.builder().scenarioId(1L).build();
            given(scenarioRepository.findByProjectIdAndScenarioSerial(100L, "01"))
                    .willReturn(Optional.of(mockScenario));

            // when & then
            assertThatThrownBy(() -> agentDispatchService.dispatchPlan(
                    testUser, 100L, "01", "존재하지 않는 시나리오", "main", 2L, "http://localhost", "TestGroup"))
                    .isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        @DisplayName("레포지토리가 없으면 RESOURCE_NOT_FOUND 예외 발생")
        void dispatchPlan_repoNotFound_throwsException() {
            // given
            Long projectId = 100L;
            String scenarioSerial = "01";
            Long targetRepoId = 2L;

            given(scenarioSequenceRepository.findByProjectIdAndScenarioSerialForUpdate(projectId, scenarioSerial))
                    .willReturn(Optional.empty());
            given(scenarioSequenceRepository.save(any(ScenarioSequence.class))).willReturn(ScenarioSequence.builder().projectId(projectId).scenarioSerial(scenarioSerial).build());

            // 시나리오 조회는 성공하지만 레포지토리 조회가 실패하는 상황
            Scenario mockScenario = Scenario.builder().scenarioId(1L).build();
            given(scenarioRepository.findByProjectIdAndScenarioSerial(projectId, scenarioSerial))
                    .willReturn(Optional.of(mockScenario));

            given(testExecutionRepository.save(any(TestExecution.class))).willAnswer(invocation -> {
                TestExecution te = invocation.getArgument(0);
                ReflectionTestUtils.setField(te, "executionId", 1L);
                return te;
            });
            given(githubRepositoryRepository.findByProjectIdAndGithubRepoId(projectId, targetRepoId)).willReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> agentDispatchService.dispatchPlan(
                    testUser, projectId, scenarioSerial, "회원가입", "main", targetRepoId, "http://localhost", "TestGroup"))
                    .isInstanceOf(CustomException.class)
                    .extracting(e -> ((CustomException) e).getErrorCode())
                    .isEqualTo(ErrorCode.RESOURCE_NOT_FOUND);
        }

        @Test
        @DisplayName("시나리오 스텝이 없으면 INVALID_ARGUMENT 예외 발생")
        void dispatchPlan_emptySteps_throwsException() {
            // given
            Long projectId = 100L;
            String scenarioSerial = "01";
            Long targetRepoId = 2L;

            given(scenarioSequenceRepository.findByProjectIdAndScenarioSerialForUpdate(projectId, scenarioSerial))
                    .willReturn(Optional.empty());
            given(scenarioSequenceRepository.save(any(ScenarioSequence.class)))
                    .willReturn(ScenarioSequence.builder().projectId(projectId).scenarioSerial(scenarioSerial).build());
            given(testExecutionRepository.save(any(TestExecution.class))).willAnswer(invocation -> {
                TestExecution te = invocation.getArgument(0);
                ReflectionTestUtils.setField(te, "executionId", 1L);
                return te;
            });

            GithubRepository mockRepo = GithubRepository.builder()
                    .repoUrl("https://github.com/school/repo.git").build();
            given(githubRepositoryRepository.findByProjectIdAndGithubRepoId(projectId,targetRepoId)).willReturn(Optional.of(mockRepo));

            Scenario mockScenario = Scenario.builder().scenarioId(1L).build();
            given(scenarioRepository.findByProjectIdAndScenarioSerial(projectId, scenarioSerial))
                    .willReturn(Optional.of(mockScenario));

            given(scenarioGuideRepository.findAllByScenarioIdOrderByStepOrder(1L))
                    .willReturn(List.of());

            // when & then
            assertThatThrownBy(() -> agentDispatchService.dispatchPlan(
                    testUser, projectId, scenarioSerial, "회원가입", "main", targetRepoId, "http://localhost", "TestGroup"))
                    .isInstanceOf(CustomException.class)
                    .extracting(e -> ((CustomException) e).getErrorCode())
                    .isEqualTo(ErrorCode.INVALID_ARGUMENT);
        }
    }

    // =======================================================
    // 2단계: dispatchTest
    // =======================================================
    @Nested
    @DisplayName("2단계 - dispatchTest")
    class DispatchTestTest {

        @Test
        @DisplayName("PLAN_COMPLETED 상태에서 테스트 실행 요청 시 상태가 TESTING으로 변경되고 AI 서버에 요청 전송")
        void dispatchTest_success() throws InterruptedException {
            // given
            Long executionId = 1L;

            User tester = User.builder()
                    .id(1L).githubId("12345").username("tester").email("tester@test.com").build();

            TestExecution execution = TestExecution.builder()
                    .executionId(executionId)
                    .projectId(100L)
                    .tester(tester)
                    .testerName("tester")
                    .scenarioSerial("01")
                    .scenarioAttempt(1)
                    .testName("회원가입")
                    .status(ExecutionStatus.PLAN_COMPLETED)
                    .build();

            given(testExecutionRepository.findActiveById(executionId)).willReturn(Optional.of(execution));

            mockWebServer.enqueue(new MockResponse().setResponseCode(202));

            // when
            agentDispatchService.dispatchTest(executionId);

            // then
            assertThat(execution.getStatus()).isEqualTo(ExecutionStatus.TESTING);

            RecordedRequest request = mockWebServer.takeRequest(5, TimeUnit.SECONDS);
            assertThat(request).isNotNull();
            assertThat(request.getMethod()).isEqualTo("POST");
            assertThat(request.getPath()).isEqualTo("/api/execute-test");

            String body = request.getBody().readUtf8();
            assertThat(body).contains("\"execution_id\":1");
            assertThat(body).contains("/api/agent/callback/test");
        }

        @Test
        @DisplayName("PLAN_COMPLETED가 아닌 상태에서 테스트 실행 요청 시 INVALID_ARGUMENT 예외 발생")
        void dispatchTest_notPlanCompleted_throwsException() {
            // given
            Long executionId = 1L;
            User tester = User.builder()
                    .id(1L).githubId("12345").username("tester").email("tester@test.com").build();

            TestExecution execution = TestExecution.builder()
                    .executionId(executionId)
                    .projectId(100L)
                    .tester(tester)
                    .testerName("tester")
                    .scenarioSerial("01")
                    .scenarioAttempt(1)
                    .testName("회원가입")
                    .status(ExecutionStatus.PLAN_GENERATING)
                    .build();
            given(testExecutionRepository.findActiveById(executionId)).willReturn(Optional.of(execution));

            // when & then
            assertThatThrownBy(() -> agentDispatchService.dispatchTest(executionId))
                    .isInstanceOf(CustomException.class)
                    .extracting(e -> ((CustomException) e).getErrorCode())
                    .isEqualTo(ErrorCode.INVALID_ARGUMENT);
        }

        @Test
        @DisplayName("존재하지 않는 executionId로 테스트 실행 요청 시 RESOURCE_NOT_FOUND 예외 발생")
        void dispatchTest_executionNotFound_throwsException() {
            // given
            Long nonExistentId = 999L;
            given(testExecutionRepository.findActiveById(nonExistentId)).willReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> agentDispatchService.dispatchTest(nonExistentId))
                    .isInstanceOf(CustomException.class)
                    .extracting(e -> ((CustomException) e).getErrorCode())
                    .isEqualTo(ErrorCode.RESOURCE_NOT_FOUND);
        }
    }
}
