package com.graduationCapstone.Probe.domain.project.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "scenario_guide")
public class ScenarioGuide {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "scenario_guide_id")
    private Long scenarioGuideId;

    @Column(name = "scenario_id", nullable = false)
    private Long scenarioId;

    @Column(name = "step_order", nullable = false)
    private Integer stepOrder;

    // 가이드 내용 조인 관계 설정 (N:1)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "guide_id", nullable = false)
    private Guide guide;
}
