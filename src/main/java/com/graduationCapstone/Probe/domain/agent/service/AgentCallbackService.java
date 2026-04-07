package com.graduationCapstone.Probe.domain.agent.service;

import com.graduationCapstone.Probe.domain.agent.dto.AgentPlanCallbackRequestDto;
import com.graduationCapstone.Probe.domain.agent.dto.AgentTestCallbackRequestDto;
import com.graduationCapstone.Probe.domain.test.entity.TestExecution;
import com.graduationCapstone.Probe.domain.test.entity.TestResult;
import com.graduationCapstone.Probe.domain.test.repository.TestExecutionRepository;
import com.graduationCapstone.Probe.domain.test.repository.TestResultRepository;
import com.graduationCapstone.Probe.global.exception.ErrorCode;
import com.graduationCapstone.Probe.global.exception.handler.CustomException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class AgentCallbackService {

    private final TestExecutionRepository testExecutionRepository;
    private final TestResultRepository testResultRepository;

    /**
     * 1단계 콜백: 테스트 계획서 생성 완료.
     * 계획서 S3 URL을 저장하고 상태를 PLAN_COMPLETED로 변경합니다.
     */
    @Transactional
    public void handlePlanCallback(AgentPlanCallbackRequestDto dto) {
        log.info("Plan callback received. ExecutionId={}", dto.executionId());

        TestExecution execution = testExecutionRepository.findById(dto.executionId())
                .orElseThrow(() -> new CustomException(ErrorCode.RESOURCE_NOT_FOUND));

        execution.completePlan(dto.planS3Url());
        testExecutionRepository.save(execution);

        log.info("Plan completed. ExecutionId={}, PlanS3Url={}", dto.executionId(), dto.planS3Url());
    }

    /**
     * 2단계 콜백: 테스트 실행 완료.
     * 산출물 URL, 소요 시간, 개별 TestResult를 저장합니다.
     */
    @Transactional
    public void handleTestCallback(AgentTestCallbackRequestDto dto) {
        log.info("Test callback received. ExecutionId={}, Status={}", dto.executionId(), dto.status());

        TestExecution execution = testExecutionRepository.findById(dto.executionId())
                .orElseThrow(() -> new CustomException(ErrorCode.RESOURCE_NOT_FOUND));

        // TestExecution 완료 처리
        execution.completeTest(dto.status(), dto.durationSeconds(),
                dto.planResultS3Url(), dto.testSpecS3Url());
        testExecutionRepository.save(execution);

        // 개별 TestResult 저장
        if (dto.results() != null && !dto.results().isEmpty()) {
            List<TestResult> results = dto.results().stream()
                    .map(r -> TestResult.builder()
                            .executionId(dto.executionId())
                            .testCaseNumber(r.testCaseNumber())
                            .caseName(r.caseName())
                            .testCodeName(r.testCodeName())
                            .status(r.status())
                            .durationSeconds(r.durationSeconds())
                            .errorLog(r.errorLog())
                            .testCode(r.testCode())
                            .scenarioDetail(r.scenarioDetail())
                            .screenshotS3Urls(r.screenshotS3Urls())
                            .build())
                    .toList();

            testResultRepository.saveAll(results);
            log.info("Saved {} TestResult(s) for ExecutionId={}", results.size(), dto.executionId());
        }
    }
}
