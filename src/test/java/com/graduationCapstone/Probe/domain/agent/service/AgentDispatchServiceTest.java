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
        ReflectionTestUtils.setField(agentDispatchService, "agentRepoOwner", "test-owner");
        ReflectionTestUtils.setField(agentDispatchService, "agentRepoName", "test-agent-repo");
    }

    @AfterEach
    void tearDown() throws IOException {
        mockWebServer.shutdown();
    }

    @Test
    @DisplayName("정상적인 Dispatch 요청 시 Github API 규격에 맞 JSON이 전송")
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
        given(scenarioGuideRepository.findAllByScenarioIdOrderByStepOrder(scenarioId)).willReturn(List.of(mockScenarioGuide));

        mockWebServer.enqueue(new MockResponse().setResponseCode(204));

        // when
        agentDispatchService.triggerAgentExecution(executionId, scenarioId, targetBranch);

        // then
        RecordedRequest request = mockWebServer.takeRequest(5, TimeUnit.SECONDS);

        assertThat(request).isNotNull();
        // 1. URL과 메서드 확인
        assertThat(request.getMethod()).isEqualTo("POST");
        assertThat(request.getPath()).isEqualTo("/repos/test-owner/test-agent-repo/dispatches");

        // 2. Body 내용 확인 (가장 중요!)
        String body = request.getBody().readUtf8();
        assertThat(body).contains("\"event_type\":\"agent-trigger\"");
        assertThat(body).contains("\"executionId\":1");
        assertThat(body).contains("\"targetBranch\":\"feature/login\"");
        assertThat(body).contains("\"test_item\":\"로그인 버튼을 누른다\"");
    }
}
