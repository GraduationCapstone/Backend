package com.graduationCapstone.Probe.domain.test.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.Where;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;
import java.util.List;

@Entity
@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "test_result")
@SQLDelete(sql = "UPDATE test_result SET deleted_at = NOW() WHERE result_id = ?")
@Where(clause = "deleted_at IS NULL")
public class TestResult {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "result_id")
    private Long resultId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "execution_id", nullable = false)
    private TestExecution testExecution; //부모 연결

    // ── ID 구성 요소 ──

    /**
     * AI가 부여한 테스트 케이스 번호 (2자리, 예: "01", "02").
     * 테스트케이스 ID = T[SCENARIO_SERIAL][SCENARIO_ATTEMPT]_[testCaseNumber]
     */
    @Column(name = "test_case_number", length = 50)
    private String testCaseNumber;

    // ── 테스트 정보 ──

    /** AI가 명명한 개별 테스트 케이스 이름 */
    @Column(name = "case_name")
    private String caseName;

    /** 수정 가능한 테스트 코드명 (대시보드에 표시) */
    @Column(name = "test_code_name", length = 200)
    private String testCodeName;

    /** PASS, BLOCK, FAIL, UNTEST */
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 10)
    private ResultStatus status;

    /** 개별 테스트 케이스 소요 시간 (초) */
    @Column(name = "duration_seconds")
    private Double durationSeconds;

    /** 에러 메시지 (PASS면 null) */
    @Column(name = "error_log", columnDefinition = "TEXT")
    private String errorLog;

    /** 테스트 코드 스니펫 (프론트엔드 상세보기에 표시) */
    @Column(name = "test_code", columnDefinition = "TEXT")
    private String testCode;

    /**
     * 테스트 시나리오 상세 데이터 (JSONB).
     * xlsx 결과 보고서에서 파싱: {시나리오명, 설명, TC ID, TC명, 전제조건, 테스트 데이터, 실행 단계, 결과}
     */
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "scenario_detail", columnDefinition = "jsonb")
    private ScenarioDetailData scenarioDetail;

    /** Playwright 스크린샷 S3 URL 목록 */
    @ElementCollection
    @CollectionTable(name = "test_result_screenshot", joinColumns = @JoinColumn(name = "result_id"))
    @Column(name = "screenshot_s3_url", length = 2048)
    private List<String> screenshotS3Urls;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    // ── Soft Delete ──

    /** null이면 활성, 값이 있으면 삭제된 레코드 */
    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;

    // ── 비즈니스 메서드 ──

    /** Soft Delete 처리 */
    public void softDelete() {
        this.deletedAt = LocalDateTime.now();
    }

    /** 삭제 여부 확인 */
    public boolean isDeleted() {
        return this.deletedAt != null;
    }

    /** 테스트 코드명 수정 */
    public void updateTestCodeName(String caseName) {
        this.caseName = caseName;
    }

    /** 테스트케이스ID 반환 */
    public String getFullTestCaseId() {
        return this.testCaseNumber;
    }
}
