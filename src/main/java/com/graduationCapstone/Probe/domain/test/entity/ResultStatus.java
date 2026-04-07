package com.graduationCapstone.Probe.domain.test.entity;

/**
 * TestResult 의 개별 테스트 케이스 결과 상태값.
 *
 * <pre>
 * PASS   → 테스트 케이스 통과
 * BLOCK  → 테스트 케이스 차단 (전제 조건 미충족 등)
 * FAIL   → 테스트 케이스 실패
 * UNTEST → 미실행 테스트 케이스
 * </pre>
 */
public enum ResultStatus {
    PASS,
    BLOCK,
    FAIL,
    UNTEST
}
