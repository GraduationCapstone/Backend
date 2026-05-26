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
                        // [시리얼 제거] "TXXXX_XX - "시리얼을 제외한 이름만 추출
                        String rawCodeName = r.testCodeName();
                        String cleanCodeName = rawCodeName;
                        if (rawCodeName != null) {
                            cleanCodeName = rawCodeName.replaceAll("^T\\d{4}_\\d{2}\\s*-\\s*", "");
                        }

                        // ScenarioDetailData 파싱
                        com.graduationCapstone.Probe.domain.test.entity.ScenarioDetailData embeddedDetail = null;
                        if (r.scenarioDetail() != null) {
                            var sd = r.scenarioDetail();
                            embeddedDetail = new com.graduationCapstone.Probe.domain.test.entity.ScenarioDetailData(
                                    sd.scenarioName(),     // 테스트 시나리오명
                                    sd.description(),      // 설명
                                    sd.testCaseId(),       // 테스트 케이스 ID
                                    r.caseName(),          // 테스트 케이스명 (대시보드 목록의 caseName 매핑)
                                    sd.precondition(),     // 전제 조건
                                    sd.testData(),         // 테스트 데이터
                                    sd.executionSteps(),   // 실행 단계
                                    sd.result()            // 결과
                            );
                        }

                        // TestResult 엔티티 객체 생성 및 정제된 값 주입
                        return TestResult.builder()
                                .testExecution(execution)
                                .testCaseNumber(r.testCaseNumber())
                                .caseName(r.caseName())
                                .testCodeName(cleanCodeName) // 정제된 이름 저장
                                .status(r.status())
                                .durationSeconds(r.durationSeconds())
                                .errorLog(r.errorLog())
                                .testCode(r.testCode())
                                .scenarioDetail(embeddedDetail) // 바인딩된 객체 주입
                                .screenshotS3Urls(r.screenshotS3Urls())
                                .build();
                    })
                    .toList();

            testResultRepository.saveAll(results);
            log.info("Saved {} TestResult(s) for ExecutionId={}", results.size(), dto.executionId());
        }
    }
}
