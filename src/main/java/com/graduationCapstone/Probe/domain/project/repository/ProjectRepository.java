package com.graduationCapstone.Probe.domain.project.repository;

import com.graduationCapstone.Probe.domain.project.entity.Project;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ProjectRepository extends JpaRepository<Project, Long> {

    @Query("SELECT DISTINCT p FROM Project p " +
            "LEFT JOIN FETCH p.projectRepos pr " +
            "LEFT JOIN FETCH pr.githubRepo " +
            "LEFT JOIN FETCH p.members m " +
            "WHERE m.user.id = :userId")
    List<Project> findAllByUserId(@Param("userId") Long userId);

    @Query("SELECT DISTINCT p FROM Project p " +
            "LEFT JOIN FETCH p.projectRepos pr " +
            "LEFT JOIN FETCH pr.githubRepo " +
            "LEFT JOIN FETCH p.members " +
            "WHERE p.id = :id")
    Optional<Project> findByIdWithRepos(@Param("id") Long id);

    @Query("SELECT DISTINCT p FROM Project p " +
            "LEFT JOIN FETCH p.members " +
            "WHERE p.id = :id")
    Optional<Project> findByIdWithMembers(@Param("id") Long id);
}