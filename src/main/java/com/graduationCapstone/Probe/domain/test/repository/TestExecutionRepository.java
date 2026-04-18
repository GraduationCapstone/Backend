package com.graduationCapstone.Probe.domain.test.repository;

import com.graduationCapstone.Probe.domain.test.entity.TestExecution;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface TestExecutionRepository extends JpaRepository<TestExecution, Long>, TestExecutionRepositoryCustom {
    long countByProjectId(Long projectId);
    List<TestExecution> findAllByTestGroup_GroupId(Long testGroupGroupId);
}
