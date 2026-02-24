package com.graduationCapstone.Probe.domain.agent.service;

import com.graduationCapstone.Probe.domain.project.entity.GithubRepository;
import com.graduationCapstone.Probe.domain.project.entity.Guide;
import com.graduationCapstone.Probe.domain.project.entity.ScenarioGuide;
import com.graduationCapstone.Probe.domain.project.repository.GithubRepositoryRepository;
import com.graduationCapstone.Probe.domain.project.repository.ScenarioGuideRepository;
import com.graduationCapstone.Probe.domain.test.entity.TestExecution;
import com.graduationCapstone.Probe.domain.test.repository.TestExecutionRepository;
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
                scenarioGuideRepository
        );

        // @Value 필드는 생성자에 없으므로 ReflectionTestUtils로 강제 주입
        // MockWebServer의 baseUrl을 serverUrl로 설정하여 callbackUrl 생성 검증
        ReflectionTestUtils.setField(agentDispatchService, "serverUrl",
                mockWebServer.url("").toString().replaceAll("/$", ""));
    }

    @AfterEach
    void tearDown() throws IOException {
        mockWebServer.shutdown();
    }

    @Test
    @DisplayName("정상적인 Dispatch 요청 시 FastAPI 규격에 맞는 JSON이 전송됨")
    void triggerAgentExecution_success() throws InterruptedException {
        // given
        Long executionId = 1L;
        Long scenarioId = 1L;
        Long projectId = 100L;
        String targetBranch = "feature/login";

        // mocking
        TestExecution mockExecution = TestExecution.builder()
                .executionId(executionId)
                .projectId(projectId)
                .build();
        given(testExecutionRepository.findById(executionId)).willReturn(Optional.of(mockExecution));

        GithubRepository mockRepo = GithubRepository.builder()
                .repoUrl("https://github.com/school/target-repo.git")
                .build();
        given(githubRepositoryRepository.findByProjectId(projectId)).willReturn(Optional.of(mockRepo));

        Guide mockGuide = Guide.builder()
                .testItem("로그인 버튼을 누른다")
                .build();
        ScenarioGuide mockScenarioGuide = ScenarioGuide.builder()
                .stepOrder(1)
                .guide(mockGuide)
                .build();
        given(scenarioGuideRepository.findAllByScenarioIdOrderByStepOrder(scenarioId))
                .willReturn(List.of(mockScenarioGuide));

        mockWebServer.enqueue(new MockResponse().setResponseCode(200));

        // when
        agentDispatchService.triggerAgentExecution(executionId, scenarioId, targetBranch);

        // then
        RecordedRequest request = mockWebServer.takeRequest(5, TimeUnit.SECONDS);

        assertThat(request).isNotNull();

        // 1. URL 및 메서드 확인
        assertThat(request.getMethod()).isEqualTo("POST");
        assertThat(request.getPath()).isEqualTo("/api/v1/test/execute");

        // 2. Body 내용 확인 (GitHub Dispatch 래핑 없이 AiTestRequest 직접 전송)
        String body = request.getBody().readUtf8();
        assertThat(body).contains("\"execution_id\":1");
        assertThat(body).contains("\"callback_url\":");
        assertThat(body).contains("/api/agent/callback");
        assertThat(body).contains("\"target_repo_url\":\"https://github.com/school/target-repo.git\"");
        assertThat(body).contains("\"target_branch\":\"feature/login\"");
        assertThat(body).contains("\"test_item\":\"로그인 버튼을 누른다\"");
        assertThat(body).contains("\"step_order\":1");

        // 3. 구 GitHub Dispatch 필드가 없는지 확인
        assertThat(body).doesNotContain("event_type");
        assertThat(body).doesNotContain("client_payload");
    }

    @Test
    @DisplayName("AI 서버 호출 실패 시 TestExecution 상태가 FAILED로 업데이트됨")
    void triggerAgentExecution_aiServerError_updatesStatusToFailed() throws InterruptedException {
        // given
        Long executionId = 2L;
        Long scenarioId = 1L;
        Long projectId = 200L;
        String targetBranch = "main";

        TestExecution mockExecution = TestExecution.builder()
                .executionId(executionId)
                .projectId(projectId)
                .status("PENDING")
                .build();
        given(testExecutionRepository.findById(executionId)).willReturn(Optional.of(mockExecution));

        GithubRepository mockRepo = GithubRepository.builder()
                .repoUrl("https://github.com/school/target-repo.git")
                .build();
        given(githubRepositoryRepository.findByProjectId(projectId)).willReturn(Optional.of(mockRepo));

        Guide mockGuide = Guide.builder().testItem("회원가입 버튼 클릭").build();
        ScenarioGuide mockScenarioGuide = ScenarioGuide.builder().stepOrder(1).guide(mockGuide).build();
        given(scenarioGuideRepository.findAllByScenarioIdOrderByStepOrder(scenarioId))
                .willReturn(List.of(mockScenarioGuide));

        // AI 서버가 500 에러를 반환하는 상황 시뮬레이션
        mockWebServer.enqueue(new MockResponse().setResponseCode(500).setBody("Internal Server Error"));

        // 에러 핸들러에서 재조회 시 사용
        given(testExecutionRepository.findById(executionId)).willReturn(Optional.of(mockExecution));

        // when
        agentDispatchService.triggerAgentExecution(executionId, scenarioId, targetBranch);

        // 비동기 처리 대기
        mockWebServer.takeRequest(5, TimeUnit.SECONDS);
        Thread.sleep(500);

        // then: save가 호출됐는지 검증 (에러 핸들러 실행 확인)
        org.mockito.Mockito.verify(testExecutionRepository, org.mockito.Mockito.atLeastOnce())
                .save(mockExecution);
    }
}
