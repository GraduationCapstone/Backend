package com.graduationCapstone.Probe.domain.test.entity;

import java.io.Serializable;

/**
 * 테스트 시나리오 상세 데이터 (JSONB로 저장).
 * xlsx 결과 보고서에서 파싱하여 저장됩니다.
 */
public record ScenarioDetailData(
        String scenarioName,      // 테스트 시나리오명
        String description,       // 설명
        String testCaseId,        // 테스트 케이스 ID
        String testCaseName,      // 테스트 케이스명
        String precondition,      // 전제 조건
        String testData,          // 테스트 데이터
        String executionSteps,    // 실행 단계
        String result             // 결과
) implements Serializable {
}
