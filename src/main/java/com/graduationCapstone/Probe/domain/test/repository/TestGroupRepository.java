package com.graduationCapstone.Probe.domain.test.repository;

import com.graduationCapstone.Probe.domain.test.entity.TestGroup;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface TestGroupRepository extends JpaRepository<TestGroup, Long> {
    boolean existsByProjectIdAndGroupName(Long projectId, String groupName);
}