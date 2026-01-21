package com.graduationCapstone.Probe.domain.agent.service;

import com.graduationCapstone.Probe.domain.agent.dto.AgentCommandDto;
import com.graduationCapstone.Probe.domain.agent.dto.AgentScenarioStep;
import com.graduationCapstone.Probe.domain.project.entity.GithubRepository;
import com.graduationCapstone.Probe.domain.project.entity.ScenarioGuide;
import com.graduationCapstone.Probe.domain.project.repository.GithubRepositoryRepository;
import com.graduationCapstone.Probe.domain.project.repository.ScenarioGuideRepository;
import com.graduationCapstone.Probe.domain.test.entity.TestExecution;
import com.graduationCapstone.Probe.domain.test.repository.TestExecutionRepository;
import com.graduationCapstone.Probe.global.exception.ErrorCode;
import com.graduationCapstone.Probe.global.exception.handler.CustomException;
import com.graduationCapstone.Probe.infrastructure.github.dto.GitHubDispatchRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class AgentDispatchService {
    private final WebClient githubWebClient;
    private final TestExecutionRepository testExecutionRepository;
    private final GithubRepositoryRepository githubRepositoryRepository;
    private final ScenarioGuideRepository scenarioGuideRepository;

    @Value("${github.owner}")
    private String agentRepoOwner;

    @Value("${github.repo}")
    private String agentRepoName;

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

        List<AgentScenarioStep> stepDtos = steps.stream()
                .map(sg -> new AgentScenarioStep(
                        sg.getStepOrder(),
                        sg.getGuide().getTestItem()
                ))
                .toList();

        AgentCommandDto payload = new AgentCommandDto(
                executionId,
                targetRepo.getRepoUrl(),
                targetBranch,
                stepDtos
        );

        // GitHub Dispatch 발송
        GitHubDispatchRequest request = new GitHubDispatchRequest("agent-trigger", payload);

        githubWebClient.post()
                .uri("/repos/{owner}/{repo}/dispatches", agentRepoOwner, agentRepoName)
                .bodyValue(request)
                .retrieve()
                .toBodilessEntity()
                .subscribe(
                        response -> log.info("Dispatch success. Status: {}", response.getStatusCode()),
                        error -> log.error("Dispatch failed: ", error)
                );
    }
}
