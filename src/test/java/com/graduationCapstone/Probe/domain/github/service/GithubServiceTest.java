package com.graduationCapstone.Probe.domain.github.service;

import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import org.junit.jupiter.api.*;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.test.StepVerifier;

import java.io.IOException;

class GithubServiceTest {

    private static MockWebServer mockWebServer;
    private static GithubService githubService;

    @BeforeAll
    static void setUp() throws IOException {
        mockWebServer = new MockWebServer();
        mockWebServer.start();
        WebClient webClient = WebClient.builder()
                .baseUrl(mockWebServer.url("/").toString())
                .build();
        githubService = new GithubService(webClient);
    }

    @AfterAll
    static void tearDown() throws IOException {
        mockWebServer.shutdown();
    }

    @Test
    @DisplayName("GitHub 목록 조회 API 연동 및 DTO 변환을 테스트합니다.")
    void getRepoListTest() {
        mockWebServer.enqueue(new MockResponse()
                .setHeader("Content-Type", "application/json")
                .setBody("[{\"name\":\"Probe\", \"owner\":{\"login\":\"user\"}}]"));

        githubService.getRepoList("token")
                .as(StepVerifier::create)
                .expectNextMatches(list -> list.getFirst().repoName().equals("Probe"))
                .verifyComplete();
    }

    @Test
    @DisplayName("GitHub 레포지토리 전체 코드 추출 로직을 검증합니다.")
    void getRepoFullCodeTest() {
        String mockBaseUrl = mockWebServer.url("/").toString();

        mockWebServer.enqueue(new MockResponse()
                .setHeader("Content-Type", "application/json")
                .setBody("{\"default_branch\":\"main\"}"));

        mockWebServer.enqueue(new MockResponse()
                .setHeader("Content-Type", "application/json")
                .setBody("{\"tree\": [{\"path\":\"src/Main.java\", \"type\":\"blob\", \"url\":\"" + mockBaseUrl + "api/blob\"}]}"));

        mockWebServer.enqueue(new MockResponse()
                .setHeader("Content-Type", "application/json")
                .setBody("public class Main {}")); // 실제 파일 내용

        // StepVerifier에 타임아웃을 걸어 무한 대기 방지
        githubService.getRepoFullCode("owner", "Probe", "token")
                .as(StepVerifier::create)
                .expectNextMatches(repoDto -> repoDto.repoName().equals("Probe"))
                .expectComplete()
                .verify(java.time.Duration.ofSeconds(5)); // 5초 안에 안 끝나면 실패 처리
    }
}