package com.graduationCapstone.Probe.domain.test.entity;

import jakarta.persistence.*;
import lombok.*;

/**
 * 프로젝트별 + 시나리오 시리얼별 SCENARIO_ATTEMPT 누적 카운터.
 *
 * <p>동일한 프로젝트에서 동일한 SCENARIO_SERIAL에 대해 테스트가 실행될 때마다
 * lastAttempt가 1씩 증가합니다. 대시보드에서 결과를 삭제하더라도 카운터는 감소하지 않습니다.</p>
 */
@Entity
@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "scenario_sequence",
        uniqueConstraints = @UniqueConstraint(columnNames = {"project_id", "scenario_serial"}))
public class ScenarioSequence {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "sequence_id")
    private Long sequenceId;

    @Column(name = "project_id", nullable = false)
    private Long projectId;

    /** 2자리 시리얼 코드 (예: "01") */
    @Column(name = "scenario_serial", nullable = false, length = 2)
    private String scenarioSerial;

    /** 누적 발급 횟수 (초기값 0, nextAttempt 호출 시 1부터 시작) */
    @Column(name = "last_attempt", nullable = false)
    @Builder.Default
    private int lastAttempt = 0;

    /**
     * 다음 SCENARIO_ATTEMPT 번호를 발급하고 반환합니다.
     *
     * @return 새로 발급된 attempt 번호 (1부터 시작)
     */
    public int nextAttempt() {
        return ++this.lastAttempt;
    }

    /**
     * SCENARIO_ATTEMPT를 2자리 문자열로 반환합니다.
     *
     * @return 포맷된 attempt (예: "01", "03")
     */
    public String getFormattedLastAttempt() {
        return String.format("%02d", this.lastAttempt);
    }
}
