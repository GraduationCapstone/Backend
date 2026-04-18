package com.graduationCapstone.Probe.domain.test.entity;

import com.graduationCapstone.Probe.domain.user.entity.User;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.List;

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

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "group_id")
    private TestGroup testGroup; // 부모 연결

    @OneToMany(mappedBy = "testExecution", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<TestResult> testResults;

    @Column(name = "project_id", nullable = false)
    private Long projectId;

    // ── 테스터 정보 ──

    /** 테스트를 실행한 사용자 (JPA 연관관계) */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_id", nullable = false)
    private User tester;

    /** 실행 시점의 사용자 이름 스냅샷 (User 삭제 시에도 유지) */
    @Column(name = "tester_name", nullable = false, length = 100)
    private String testerName;

    // ── 시나리오 ID 구성 요소 ──

    /** 시나리오 시리얼 코드 (예: "01" = 회원가입) */
    @Column(name = "scenario_serial", nullable = false, length = 2)
    private String scenarioSerial;

    /** 동일 시나리오 시리얼에 대한 누적 실행 순번 (예: 3 → "03") */
    @Column(name = "scenario_attempt", nullable = false)
    private int scenarioAttempt;

    /** 시나리오 가이드 이름 (예: "회원가입", "로그인") - 대시보드 테스트명 */
    @Column(name = "test_name", nullable = false, length = 100)
    private String testName;

    // ── 상태 및 시간 ──

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private ExecutionStatus status;

    /** 테스트 총 소요 시간 (초) */
    @Column(name = "duration_seconds")
    private Double durationSeconds;

    @CreationTimestamp
    @Column(name = "triggered_at", nullable = false, updatable = false)
    private LocalDateTime triggeredAt;

    @Column(name = "completed_at")
    private LocalDateTime completedAt;

    // ── 산출물 URL ──

    /** 테스트 계획서 (plan.xlsx) S3 URL */
    @Column(name = "plan_s3_url", length = 2048)
    private String planS3Url;

    /** 테스트 결과 보고서 (plan_result.xlsx) S3 URL */
    @Column(name = "plan_result_s3_url", length = 2048)
    private String planResultS3Url;

    /** 테스트 스펙 파일 (test.spec.js) S3 URL */
    @Column(name = "test_spec_s3_url", length = 2048)
    private String testSpecS3Url;

    // ── Soft Delete ──

    /** null이면 활성, 값이 있으면 삭제된 레코드 */
    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;

    // ── 비즈니스 메서드 ──

    /**
     * 테스트 시나리오 ID를 생성합니다.
     * 형식: T[SCENARIO_SERIAL][SCENARIO_ATTEMPT] (예: T0103)
     */
    public String getTestScenarioId() {
        return String.format("T%s%02d", scenarioSerial, scenarioAttempt);
    }

    /** 상태만 단순 변경 (예: FAILED 처리) */
    public void updateStatus(ExecutionStatus status) {
        this.status = status;
    }

    /**
     * 1단계 콜백: 테스트 계획서 생성 완료 처리.
     * 상태를 PLAN_COMPLETED로 변경하고 계획서 S3 URL을 저장합니다.
     */
    public void completePlan(String planS3Url) {
        this.status = ExecutionStatus.PLAN_COMPLETED;
        this.planS3Url = planS3Url;
    }

    /**
     * 2단계 콜백: 테스트 실행 완료 처리.
     * 상태, 소요 시간, 산출물 URL들, 완료 시각을 한번에 갱신합니다.
     */
    public void completeTest(ExecutionStatus status, Double durationSeconds,
                             String planResultS3Url, String testSpecS3Url) {
        this.status = status;
        this.durationSeconds = durationSeconds;
        this.planResultS3Url = planResultS3Url;
        this.testSpecS3Url = testSpecS3Url;
        this.completedAt = LocalDateTime.now();
    }

    /** Soft Delete 처리 */
    public void softDelete() {
        this.deletedAt = LocalDateTime.now();
    }

    /** 삭제 여부 확인 */
    public boolean isDeleted() {
        return this.deletedAt != null;
    }

    /** 테스트명(코드명) 수정 */
    public void updateTestName(String testName) {
        this.testName = testName;
    }

    /** 테스트그룹명 수정 **/
    public void updateGroupAndName(TestGroup group, String name) {
        this.testGroup = group;
        this.testName = name;
    }
}
