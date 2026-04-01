package com.graduationCapstone.Probe.domain.test.entity;

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
@Table(name = "test_result")
public class TestResult {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "result_id")
    private Long resultId;

    @Column(name = "execution_id", nullable = false)
    private Long executionId;

    // AI가 명명한 개별 테스트 케이스 이름
    @Column(name = "case_name")
    private String caseName;

    // SUCCESS, FAIL
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private ResultStatus status;

    // 에러 메시지나 실행 로그
    @Column(name = "error_log", columnDefinition = "TEXT")
    private String errorLog;

    // Playwright 스크린샷 S3 URL 목록
    @ElementCollection
    @CollectionTable(name = "test_result_screenshot", joinColumns = @JoinColumn(name = "result_id"))
    @Column(name = "screenshot_s3_url", length = 2048)
    private List<String> screenshotS3Urls;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
}

