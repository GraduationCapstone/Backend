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

    @Column(name = "report_s3_url", length = 2048)
    private String reportS3Url;

    /** 상태만 단순 변경 (예: FAILED 처리) */
    public void updateStatus(ExecutionStatus status) {
        this.status = status;
    }

    /** 테스트 완료 처리: 상태, 소요 시간, 리포트 URL, 완료 시각을 한번에 갱신 */
    public void complete(ExecutionStatus status, Long durationMs, String reportS3Url) {
        this.status = status;
        this.durationMs = durationMs;
        this.reportS3Url = reportS3Url;
        this.completedAt = java.time.LocalDateTime.now();
    }
}
