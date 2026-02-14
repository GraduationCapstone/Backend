package com.graduationCapstone.Probe.domain.project.repository;

import com.graduationCapstone.Probe.domain.project.entity.ProjectMember;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface ProjectMemberRepository extends JpaRepository<ProjectMember, Long> {
    Optional<ProjectMember> findByProjectIdAndUserId(Long projectId, Long userId);
}