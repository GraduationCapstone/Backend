package com.graduationCapstone.Probe.domain.project.repository;

import com.graduationCapstone.Probe.domain.project.entity.ProjectRepo;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface ProjectRepoRepository extends JpaRepository<ProjectRepo, Long> {

    @Query("SELECT pr FROM ProjectRepo pr " +
            "JOIN FETCH pr.githubRepo " +
            "WHERE pr.project.id IN :projectIds")
    List<ProjectRepo> findAllByProjectIdIn(@Param("projectIds") List<Long> projectIds);
}
