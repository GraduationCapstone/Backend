package com.graduationCapstone.Probe.domain.test.repository;

import com.graduationCapstone.Probe.domain.test.entity.ScenarioSequence;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface ScenarioSequenceRepository extends JpaRepository<ScenarioSequence, Long> {

    /**
     * 프로젝트 + 시나리오 시리얼 조합으로 시퀀스를 조회합니다.
     * 동시성 제어를 위해 PESSIMISTIC_WRITE 락을 사용합니다.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT ss FROM ScenarioSequence ss WHERE ss.projectId = :projectId AND ss.scenarioSerial = :scenarioSerial")
    Optional<ScenarioSequence> findByProjectIdAndScenarioSerialForUpdate(
            @Param("projectId") Long projectId,
            @Param("scenarioSerial") String scenarioSerial
    );

    Optional<ScenarioSequence> findByProjectIdAndScenarioSerial(Long projectId, String scenarioSerial);
}
