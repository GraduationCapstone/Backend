package com.graduationCapstone.Probe.domain.test.repository;

import com.graduationCapstone.Probe.domain.test.entity.TestExecution;

import java.util.List;
import java.util.Optional;

public interface TestExecutionRepositoryCustom {

    /** 프로젝트별 활성 시나리오 목록 (동적 정렬 지원) - 시나리오 대시보드용 */
    List<TestExecution> findAllActiveByProjectId(Long projectId, String sortField, boolean ascending);

    /** Soft Delete 필터링된 단건 조회 */
    Optional<TestExecution> findActiveById(Long executionId);
}
