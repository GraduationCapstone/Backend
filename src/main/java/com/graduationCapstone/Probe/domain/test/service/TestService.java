package com.graduationCapstone.Probe.domain.test.service;

import com.graduationCapstone.Probe.domain.agent.service.AgentDispatchService;
import com.graduationCapstone.Probe.domain.test.dto.*;
import com.graduationCapstone.Probe.domain.test.entity.*;
import com.graduationCapstone.Probe.domain.test.repository.*;
import com.graduationCapstone.Probe.domain.user.entity.User;
import com.graduationCapstone.Probe.global.exception.ErrorCode;
import com.graduationCapstone.Probe.global.exception.handler.CustomException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.text.DecimalFormat;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class TestService {

    private final TestGroupRepository groupRepo;
    private final TestExecutionRepository executionRepo;
    private final TestResultRepository resultRepo;
    private final AgentDispatchService agentDispatchService;

    /**
     * 사용자가 입력한 그룹명으로 바구니를 만들고, 에이전트에게 개별 테스트 생성을 위임합니다.
     * 시나리오가 1개여도 동일한 '그룹화' 과정을 거칩니다.
     */
    @Transactional
    public List<Long> createTestEntities(User user, Long projectId, TestGroupCreateRequestDto dto) {
        if (isGroupNameDuplicate(projectId, dto.baseTestGroupName())) {
            log.warn("[DUPLICATE] 이미 존재하는 그룹명입니다: {}", dto.baseTestGroupName());
            throw new CustomException(ErrorCode.DUPLICATE_TESTGROUP_NAME);
        }

        log.info("[FLOW] 테스트 그룹 생성 및 실행 시작 - ProjectID: {}, Group: {}", projectId, dto.baseTestGroupName());

        // 사용자가 입력한 그룹명으로 TestGroup을 먼저 저장합니다.
        TestGroup group = groupRepo.save(TestGroup.builder()
                .projectId(projectId)
                .groupName(dto.baseTestGroupName())
                .targetRepoId(dto.targetRepoId())
                .targetBranch(dto.targetBranch())
                .optionalServerUrl(dto.optionalServerUrl())
                .build());

        List<Long> executionIds = new java.util.ArrayList<>();

        // 선택된 시나리오 리스트를 순회하며 에이전트에게 생성/발송을 요청합니다.
        for (String serial : dto.scenarioSerials()) {
            ScenarioSerial serialInfo = ScenarioSerial.fromCode(serial);

            // AgentDispatchService가 스스로 TestExecution을 생성하고 AI에 요청을 보냅니다.
            // 여기서는 그 결과로 만들어진 ID만 받습니다. (시퀀스 중복 방지)
            Long executionId = agentDispatchService.dispatchPlan(
                    user,
                    projectId,
                    serial,
                    serialInfo.getTestItem(),
                    dto.targetBranch(),
                    dto.targetRepoId(),
                    dto.optionalServerUrl(),
                    dto.baseTestGroupName()
            );

            // 에이전트가 만든 실행 엔티티를 찾아 그룹과 이름을 동기화합니다.
            TestExecution execution = executionRepo.findById(executionId)
                    .orElseThrow(() -> new CustomException(ErrorCode.RESOURCE_NOT_FOUND));

            // 테스트 그룹 엔티티와 그룹명 스냅샷을 강제로 주입하여 교정합니다.
            execution.updateGroupAndName(group, group.getGroupName());

            executionIds.add(executionId);

            log.info("[FLOW] 에이전트 요청 완료 및 후처리 성공 - ExecutionID: {}, Scenario: {}",
                    executionId, serialInfo.getTestItem());
        }
        log.info("[FLOW] 모든 시나리오에 대한 그룹 테스트 요청 완료");
        return executionIds;
    }

    /**
     * 테스트 계획서/실행 상태 조회
     */
    @Transactional(readOnly = true)
    public String getExecutionStatus(Long projectId, Long executionId) {
        TestExecution execution = executionRepo.findById(executionId)
                .orElseThrow(() -> new CustomException(ErrorCode.RESOURCE_NOT_FOUND));

        if (!execution.getProjectId().equals(projectId)) {
            throw new CustomException(ErrorCode.ACCESS_DENIED);
        }

        // status Enum의 name을 반환 (PLAN_GENERATING, PLAN_COMPLETED 등)
        return execution.getStatus().name();
    }

    /**
     * 테스트 그룹명을 수정하고, 해당 그룹에 속한 모든 테스트 코드(Execution)의 testName 스냅샷을 동기화합니다.
     */
    @Transactional
    public void updateGroupName(Long projectId, Long groupId, String newName) {
        log.info("[UPDATE] 테스트 그룹명 수정 요청 - GroupID: {}, NewName: {}", groupId, newName);

        TestGroup group = groupRepo.findById(groupId)
                .orElseThrow(() -> new CustomException(ErrorCode.RESOURCE_NOT_FOUND));

        if (!group.getProjectId().equals(projectId)) {
            throw new CustomException(ErrorCode.ACCESS_DENIED);
        }

        group.updateGroupName(newName);
        log.info("[UPDATE] 테스트 그룹명 수정 완료 (과거 실행 스냅샷은 유지됨) - GroupID: {}", groupId);
    }

    /**
     * 테스트 그룹을 삭제합니다.
     */
    @Transactional
    public void deleteGroup(Long projectId, Long groupId) {
        log.info("[DELETE] 테스트 그룹 삭제 요청 - GroupID: {}", groupId);

        TestGroup group = groupRepo.findById(groupId)
                .orElseThrow(() -> new CustomException(ErrorCode.RESOURCE_NOT_FOUND));

        if (!group.getProjectId().equals(projectId)) {
            throw new CustomException(ErrorCode.INVALID_ARGUMENT);
        }

        group.softDelete();
        log.info("[DELETE] 테스트 그룹 및 하위 실행/결과 Soft Delete 완료 - GroupID: {}", groupId);
    }

    /**
     * 프로젝트 내 테스트 그룹 이름 중복 여부를 확인합니다.
     */
    @Transactional(readOnly = true)
    public boolean isGroupNameDuplicate(Long projectId, String groupName) {
        return groupRepo.existsByProjectIdAndGroupName(projectId, groupName);
    }

    /**
     * [Basic 목록] 프로젝트 내 테스트 그룹 요약 정보 조회
     * 반환 규격: groupId, testCaseId, testGroupName, passRatio, duration, tester, testerProfileImage, completedAt
     */
    @Transactional(readOnly = true)
    public List<TestExecutionListDto> getBasicSortedList(Long projectId, String field, boolean ascending) {
        log.info("[LIST] Basic 요약 목록 조회 - ProjectID: {}", projectId);

        List<TestExecution> executions = executionRepo.findAllActiveByProjectId(projectId, field, ascending);

        // 통계 일괄 조회를 위해 ID 추출
        List<Long> execIds = executions.stream().map(TestExecution::getExecutionId).toList();
        Map<Long, Map<ResultStatus, Long>> batchStats = resultRepo.findCountsByExecutionIds(execIds);

        return executions.stream()
                .map(e -> {
                    Map<ResultStatus, Long> counts = batchStats.getOrDefault(e.getExecutionId(), Map.of());
                    long pass = counts.getOrDefault(ResultStatus.PASS, 0L);
                    long total = counts.values().stream().mapToLong(l -> l).sum();

                    return new TestExecutionListDto(
                            e.getExecutionId(),
                            e.getTestGroup() != null ? e.getTestGroup().getGroupId() : null,
                            e.getTestScenarioId(),
                            e.getTestGroup() != null ? e.getTestGroup().getGroupName() : e.getTestName(),
                            calculateRatio(pass, total),
                            formatDuration(e.getDurationSeconds()),
                            e.getTesterName(),
                            e.getTester() != null ? e.getTester().getProfileImageUrl() : null,
                            e.getCompletedAt()
                    );
                }).toList();
    }

    /**
     * [Summary 목록] 테스트 결과 상세 목록 조회
     * 반환 규격: resultId, testCaseId, testCodeName, status, duration, tester, testerProfileImage, completedAt
     */
    @Transactional(readOnly = true)
    public List<TestCaseSummaryDto> getTestSummaryList(Long projectId, String sortField, boolean ascending) {
        log.info("[LIST] Summary 상세 결과 조회 - ProjectID: {}", projectId);

        List<TestExecution> executions = executionRepo.findAllActiveWithResultsByProjectId(projectId, sortField, ascending);

        return executions.stream()
                .flatMap(exec -> exec.getTestResults().stream()
                        .filter(res -> !res.isDeleted())
                        .map(res -> new TestCaseSummaryDto(
                                res.getResultId(),
                                res.getFullTestCaseId(),
                                res.getCaseName(),
                                res.getStatus().name(),
                                formatDuration(res.getDurationSeconds()),
                                exec.getTesterName(),
                                exec.getTester() != null ? exec.getTester().getProfileImageUrl() : null,
                                exec.getCompletedAt()
                        ))
                ).toList();
    }

    /**
     * 프로젝트 전체의 테스트 통과 비율을 조회합니다.
     */
    @Transactional(readOnly = true)
    public PassRateResponseDto getProjectGlobalStats(Long projectId) {
        Map<ResultStatus, Long> counts = resultRepo.countByStatusForProject(projectId);
        long pass = counts.getOrDefault(ResultStatus.PASS, 0L);
        long total = counts.values().stream().mapToLong(l -> l).sum();

        String countString = pass + "/" + total;
        return new PassRateResponseDto(pass, total, countString, calculateRatio(pass, total));
    }

    /**
     * 특정 테스트 코드(Execution)의 통과 비율을 계산합니다.
     */
    @Transactional(readOnly = true)
    public PassRateResponseDto getPassRateStats(Long executionId) {
        Map<ResultStatus, Long> counts = resultRepo.countByStatusForExecution(executionId);
        long pass = counts.getOrDefault(ResultStatus.PASS, 0L);
        long total = counts.values().stream().mapToLong(l -> l).sum();

        String countString = pass + "/" + total;
        return new PassRateResponseDto(pass, total, countString, calculateRatio(pass, total));
    }

    /**
     * 통과된 테스트의 비율을 소수점 첫째 자리까지 계산하여 문자열로 반환합니다.
     */
    public String calculateRatio(long pass, long total) {
        if (total == 0) return "0.0%";
        double ratio = (double) pass / total * 100;
        return String.format("%.1f%%", Math.round(ratio * 10.0) / 10.0);
    }

    /**
     * 소요 시간(초)을 문자열로 변환합니다.
     */
    public String formatDuration(Double sec) {
        if (sec == null || sec == 0) return "0s";

        int minutes = (int) (sec / 60);
        double seconds = sec % 60;

        DecimalFormat df = new DecimalFormat("0.#");

        return (minutes == 0) ? df.format(seconds) + "s" : String.format("%dm %ss", minutes, df.format(seconds));
    }

    /**
     * 날짜별 평균 테스트 시간 조회
     */
    @Transactional(readOnly = true)
    public List<DailyAvgDurationResponseDto> getDailyAvgDurations(Long projectId) {
        return executionRepo.findDailyAvgDuration(projectId).stream()
                .map(t -> new DailyAvgDurationResponseDto(t.get(0, String.class), formatDuration(t.get(1, Double.class))))
                .toList();
    }

    /**
     * 프로젝트 내의 테스트 케이스(Execution) 총 개수 조회
     */
    @Transactional(readOnly = true)
    public long countTotalExecutions(Long projectId) {
        return executionRepo.countByProjectIdAndDeletedAtIsNull(projectId);
    }

    /**
     * 특정 테스트 그룹에 포함된 모든 하위 결과의 총 개수 집계
     */
    @Transactional(readOnly = true)
    public long countResultsInGroup(Long groupId) {
        return resultRepo.countByTestExecution_TestGroup_GroupIdAndDeletedAtIsNull(groupId);
    }
}