package com.graduationCapstone.Probe.domain.agent.service;

import com.graduationCapstone.Probe.domain.agent.dto.AgentPlanCallbackRequestDto;
import com.graduationCapstone.Probe.domain.agent.dto.AgentTestCallbackRequestDto;
import com.graduationCapstone.Probe.domain.test.entity.ExecutionStatus;
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

    private static final java.util.Set<ExecutionStatus> PLAN_CALLBACK_ALLOWED =
            java.util.EnumSet.of(ExecutionStatus.PLAN_GENERATING, ExecutionStatus.FAILED);

    private static final java.util.Set<ExecutionStatus> TEST_CALLBACK_ALLOWED =
            java.util.EnumSet.of(ExecutionStatus.TESTING, ExecutionStatus.FAILED);

    /**
     * 1단계 콜백: 테스트 계획서 생성 완료.
     * 계획서 S3 URL을 저장하고 상태를 PLAN_COMPLETED로 변경합니다.
     *
     * <p>허용 상태: PLAN_GENERATING, FAILED (네트워크 오류 후 복구 허용).
     * 이미 PLAN_COMPLETED/TESTING/COMPLETED인 경우 중복 콜백으로 간주하고 무시합니다.</p>
     */
    @Transactional
    public void handlePlanCallback(AgentPlanCallbackRequestDto dto) {
        log.info("Plan callback received. ExecutionId={}", dto.executionId());

        TestExecution execution = testExecutionRepository.findById(dto.executionId())
                .orElseThrow(() -> new CustomException(ErrorCode.RESOURCE_NOT_FOUND));

        ExecutionStatus currentStatus = execution.getStatus();
        if (!PLAN_CALLBACK_ALLOWED.contains(currentStatus)) {
            log.warn("Plan callback ignored. ExecutionId={}, CurrentStatus={} (expected PLAN_GENERATING or FAILED)",
                    dto.executionId(), currentStatus);
            return;
        }

        if (currentStatus == ExecutionStatus.FAILED) {
            log.info("Recovering from FAILED state via plan callback. ExecutionId={}", dto.executionId());
        }

        execution.completePlan(dto.planS3Url());
        testExecutionRepository.save(execution);

        log.info("Plan completed. ExecutionId={}, PlanS3Url={}", dto.executionId(), dto.planS3Url());
    }

    /**
     * 2단계 콜백: 테스트 실행 완료.
     * 산출물 URL, 소요 시간, 개별 TestResult를 저장합니다.
     *
     * <p>허용 상태: TESTING, FAILED (네트워크 오류 후 복구 허용).
     * 이미 COMPLETED인 경우 중복 콜백으로 간주하고 무시합니다.</p>
     */
    @Transactional
    public void handleTestCallback(AgentTestCallbackRequestDto dto) {
        log.info("Test callback received. ExecutionId={}, Status={}", dto.executionId(), dto.status());

        TestExecution execution = testExecutionRepository.findById(dto.executionId())
                .orElseThrow(() -> new CustomException(ErrorCode.RESOURCE_NOT_FOUND));

        ExecutionStatus currentStatus = execution.getStatus();
        if (!TEST_CALLBACK_ALLOWED.contains(currentStatus)) {
            log.warn("Test callback ignored. ExecutionId={}, CurrentStatus={} (expected TESTING or FAILED)",
                    dto.executionId(), currentStatus);
            return;
        }

        if (currentStatus == ExecutionStatus.FAILED) {
            log.info("Recovering from FAILED state via test callback. ExecutionId={}", dto.executionId());
        }

        // TestExecution 완료 처리
        execution.completeTest(dto.status(), dto.durationSeconds(),
                dto.planResultS3Url(), dto.testSpecS3Url());
        testExecutionRepository.save(execution);

        // 개별 TestResult 저장
        if (dto.results() != null && !dto.results().isEmpty()) {
            List<TestResult> results = dto.results().stream()
                    .map(r -> {
                        // caseName과 testCodeName 모두에서 시리얼 제거
                        String cleanCaseName = r.caseName();
                        String cleanTestCodeName = r.testCodeName();

                        // 시리얼(TXXXX_XX - ) 제거
                        String serial = "^T\\d{4}_\\d{2}\\s*-\\s*";

                        if (cleanCaseName != null) {
                            cleanCaseName = cleanCaseName.replaceAll(serial, "");
                        }
                        if (cleanTestCodeName != null) {
                            cleanTestCodeName = cleanTestCodeName.replaceAll(serial, "");
                        }

                        // 변환 후에도 비어있는 경우 방지
                        if (cleanCaseName == null || cleanCaseName.isBlank()) cleanCaseName = cleanTestCodeName;
                        if (cleanTestCodeName == null || cleanTestCodeName.isBlank()) cleanTestCodeName = cleanCaseName;

                        // ScenarioDetailData 객체
                        com.graduationCapstone.Probe.domain.test.entity.ScenarioDetailData embeddedDetail = null;
                        if (r.scenarioDetail() != null) {
                            var sd = r.scenarioDetail();
                            embeddedDetail = new com.graduationCapstone.Probe.domain.test.entity.ScenarioDetailData(
                                    sd.scenarioName(),
                                    sd.description(),
                                    sd.testCaseId(),
                                    cleanCaseName,
                                    sd.precondition(),
                                    sd.testData(),
                                    sd.executionSteps(),
                                    sd.result()
                            );
                        }

                        return TestResult.builder()
                                .testExecution(execution)
                                .testCaseNumber(r.testCaseNumber())
                                .caseName(cleanCaseName)
                                .testCodeName(cleanTestCodeName)
                                .status(r.status())
                                .durationSeconds(r.durationSeconds())
                                .errorLog(r.errorLog())
                                .testCode(r.testCode())
                                .scenarioDetail(embeddedDetail)
                                .screenshotS3Urls(r.screenshotS3Urls())
                                .build();
                    })
                    .toList();

            testResultRepository.saveAll(results);
            log.info("Saved {} TestResult(s) for ExecutionId={}", results.size(), dto.executionId());
        }
    }
}
