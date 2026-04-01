package com.graduationCapstone.Probe.domain.agent.service;

import com.graduationCapstone.Probe.domain.agent.dto.AgentCallbackRequestDto;
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

    @Transactional
    public void handleCallback(AgentCallbackRequestDto dto) {
        log.info("Callback received. ExecutionId={}, Status={}", dto.executionId(), dto.status());

        TestExecution execution = testExecutionRepository.findById(dto.executionId())
                .orElseThrow(() -> new CustomException(ErrorCode.RESOURCE_NOT_FOUND));


        // TestExecution 완료 처리
        execution.complete(dto.status(), dto.durationMs(),
                dto.planS3Url(), dto.planResultS3Url(), dto.testSpecS3Url());
        testExecutionRepository.save(execution);

        // 개별 TestResult 저장
        if (dto.results() != null && !dto.results().isEmpty()) {
            List<TestResult> results = dto.results().stream()
                    .map(r -> TestResult.builder()
                            .executionId(dto.executionId())
                            .caseName(r.caseName())
                            .status(r.status())
                            .errorLog(r.errorLog())
                            .screenshotS3Urls(r.screenshotS3Urls())
                            .build())
                    .toList();

            testResultRepository.saveAll(results);
            log.info("Saved {} TestResult(s) for ExecutionId={}", results.size(), dto.executionId());
        }
    }
}

