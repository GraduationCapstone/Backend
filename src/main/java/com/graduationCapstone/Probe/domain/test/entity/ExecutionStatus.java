package com.graduationCapstone.Probe.domain.test.entity;

/**
 * TestExecution 의 생명주기 상태값 (2단계 분리 반영).
 *
 * <pre>
 * ── 1단계: 테스트 계획서 생성 ──
 * PLAN_GENERATING  → AI 서버에 테스트 계획서 생성 요청됨
 * PLAN_COMPLETED   → 계획서 생성 완료 (계획서 다운로드 가능, 테스트 시작 가능)
 *
 * ── 2단계: 테스트 실행 ──
 * TESTING          → 테스트 실행 중 (프론트에서 코드 생성 중/완료 UI 연출)
 * COMPLETED        → 테스트 완료
 *
 * ── 공통 ──
 * FAILED           → AI 서버 호출 실패 또는 오류 발생
 * </pre>
 */
public enum ExecutionStatus {
    PLAN_GENERATING,
    PLAN_COMPLETED,
    TESTING,
    COMPLETED,
    FAILED
}
