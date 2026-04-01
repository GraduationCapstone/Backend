package com.graduationCapstone.Probe.domain.test.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.ColumnDefault;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "test_execution")
public class TestExecution {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "execution_id")
    private Long executionId;

    @Column(name = "project_id", nullable = false)
    private Long projectId;

    @Column(name = "member_id", nullable = false)
    private Long memberId;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 15)
    @ColumnDefault("'PENDING'")
    private ExecutionStatus status;

    @Column(name = "duration_ms")
    private Long durationMs;

    // 트리거된 시간
    @CreationTimestamp
    @Column(name = "triggered_at", nullable = false, updatable = false)
    private LocalDateTime triggeredAt;

    @Column(name = "completed_at")
    private LocalDateTime completedAt;

    // 산출물 URL들
    @Column(name = "plan_s3_url", length = 2048)
    private String planS3Url;           // plan.xlsx

    @Column(name = "plan_result_s3_url", length = 2048)
    private String planResultS3Url;     // plan_result.xlsx

    @Column(name = "test_spec_s3_url", length = 2048)
    private String testSpecS3Url;       // test.spec.js

    /** 상태만 단순 변경 (예: FAILED 처리) */
    public void updateStatus(ExecutionStatus status) {
        this.status = status;
    }

    /** 테스트 완료 처리: 상태, 소요 시간, 산출물 URL들, 완료 시각을 한번에 갱신 */
    public void complete(ExecutionStatus status, Long durationMs,
                         String planS3Url, String planResultS3Url, String testSpecS3Url) {
        this.status = status;
        this.durationMs = durationMs;
        this.planS3Url = planS3Url;
        this.planResultS3Url = planResultS3Url;
        this.testSpecS3Url = testSpecS3Url;
        this.completedAt = java.time.LocalDateTime.now();
    }
}
