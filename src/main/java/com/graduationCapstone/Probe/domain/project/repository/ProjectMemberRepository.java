package com.graduationCapstone.Probe.domain.project.repository;

import com.graduationCapstone.Probe.domain.project.entity.ProjectMember;
import com.graduationCapstone.Probe.domain.project.entity.ProjectRole;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ProjectMemberRepository extends JpaRepository<ProjectMember, Long> {

    @Query("SELECT pm FROM ProjectMember pm " +
            "JOIN FETCH pm.user " +
            "WHERE pm.project.id = :projectId AND pm.user.id = :userId")
    Optional<ProjectMember> findByProjectIdAndUserId(@Param("projectId") Long projectId, @Param("userId") Long userId);

    @Query("SELECT COUNT(pm) > 0 FROM ProjectMember pm " +
            "WHERE pm.project.id = :projectId " +
            "AND pm.user.id = :userId " +
            "AND pm.role = :role")
    boolean existsByProjectIdAndUserIdAndRole(@Param("projectId") Long projectId, @Param("userId") Long userId, @Param("role") ProjectRole role);

    @Query("SELECT pm FROM ProjectMember pm " +
            "JOIN FETCH pm.user " +
            "WHERE pm.project.id IN :projectIds")
    List<ProjectMember> findAllByProjectIdIn(@Param("projectIds") List<Long> projectIds);
}