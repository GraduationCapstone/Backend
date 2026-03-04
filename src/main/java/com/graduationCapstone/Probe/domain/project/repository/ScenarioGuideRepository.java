package com.graduationCapstone.Probe.domain.project.repository;

import com.graduationCapstone.Probe.domain.project.entity.ScenarioGuide;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface ScenarioGuideRepository extends JpaRepository<ScenarioGuide, Long> {

    // 특정 시나리오 ID에 해당하는 가이드 목록을 순서대로(ASC) 조회
    // N+1 문제를 방지하기 위해 Guide 엔티티를 Fetch Join
    @Query("SELECT sg FROM ScenarioGuide sg JOIN FETCH sg.guide WHERE sg.scenarioId = :scenarioId ORDER BY sg.stepOrder ASC")
    List<ScenarioGuide> findAllByScenarioIdOrderByStepOrder(@Param("scenarioId") Long scenarioId);
}
