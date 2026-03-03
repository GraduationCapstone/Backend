package com.graduationCapstone.Probe.domain.test.entity;

/**
 * TestExecution 의 생명주기 상태값.
 *
 * <pre>
 * PENDING   → 테스트 실행 요청 직후 (AI 서버에 dispatch 되기 전)
 * RUNNING   → AI 서버가 테스트를 실행 중
 * COMPLETED → AI 서버가 테스트를 정상 완료하고 콜백 수신됨
 * FAILED    → AI 서버 호출 실패 또는 테스트 실행 중 오류 발생
 * </pre>
 */
public enum ExecutionStatus {
    PENDING,
    RUNNING,
    COMPLETED,
    FAILED
}

