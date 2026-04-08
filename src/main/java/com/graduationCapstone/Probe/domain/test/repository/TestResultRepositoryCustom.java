package com.graduationCapstone.Probe.domain.test.repository;

import com.graduationCapstone.Probe.domain.test.entity.ResultStatus;
import com.graduationCapstone.Probe.domain.test.entity.TestResult;

import java.util.List;
import java.util.Map;
import java.util.Optional;

public interface TestResultRepositoryCustom {

    /** execution별 활성 테스트 결과 목록 (동적 정렬 지원) - 테스트케이스 대시보드용 */
    List<TestResult> findAllActiveByExecutionId(Long executionId, String sortField, boolean ascending);

    /** Soft Delete 필터링된 단건 조회 */
    Optional<TestResult> findActiveById(Long resultId);

    /** 프로젝트 전체의 상태별 테스트 결과 개수 (Pass/Block/Fail/Untest 비율 계산용) */
    Map<ResultStatus, Long> countByStatusForProject(Long projectId);

    /** 특정 execution의 상태별 테스트 결과 개수 */
    Map<ResultStatus, Long> countByStatusForExecution(Long executionId);
}
