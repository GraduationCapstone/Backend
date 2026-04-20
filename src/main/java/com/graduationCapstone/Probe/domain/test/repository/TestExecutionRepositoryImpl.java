package com.graduationCapstone.Probe.domain.test.repository;

import com.graduationCapstone.Probe.domain.test.entity.*;
import com.querydsl.core.Tuple;
import com.querydsl.core.types.OrderSpecifier;
import com.querydsl.core.types.dsl.ComparableExpressionBase;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class TestExecutionRepositoryImpl implements TestExecutionRepositoryCustom {

    private final JPAQueryFactory queryFactory;
    private static final QTestExecution te = QTestExecution.testExecution;
    private static final QTestResult tr = QTestResult.testResult;
    private static final QTestGroup tg = QTestGroup.testGroup;

    @Override
    public List<TestExecution> findAllActiveByProjectId(Long projectId, String sortField, boolean ascending) {
        return queryFactory
                .selectFrom(te)
                .where(
                        te.projectId.eq(projectId),
                        te.deletedAt.isNull()
                )
                .orderBy(buildOrderSpecifier(sortField, ascending))
                .fetch();
    }

    @Override
    public Optional<TestExecution> findActiveById(Long executionId) {
        TestExecution result = queryFactory
                .selectFrom(te)
                .where(
                        te.executionId.eq(executionId),
                        te.deletedAt.isNull()
                )
                .fetchOne();
        return Optional.ofNullable(result);
    }

    @Override
    public List<Tuple> findDailyAvgDuration(Long projectId) {
        return queryFactory
                .select(
                        te.completedAt.stringValue().substring(0, 10),
                        te.durationSeconds.avg()
                )
                .from(te)
                .where(
                        te.projectId.eq(projectId),
                        te.status.eq(ExecutionStatus.COMPLETED),
                        te.deletedAt.isNull(),      // 삭제된 실행은 통계에서 제외
                        te.completedAt.isNotNull()  // substring 연산을 위한 Null 방어
                )
                .groupBy(te.completedAt.stringValue().substring(0, 10))
                .fetch();
    }

    @Override
    public List<TestExecution> findAllActiveWithResultsByProjectId(Long projectId, String sortField, boolean ascending) {
        return queryFactory
                .selectFrom(te)
                // 페이징 구현 시 defaoult_batch_fetch_size 설정 필요, 필요 시 수정 예정
                .leftJoin(te.testResults, tr).fetchJoin() // 결과 미리 로딩
                .leftJoin(te.testGroup, tg).fetchJoin()   // 그룹 정보 미리 로딩
                .where(te.projectId.eq(projectId), te.deletedAt.isNull())
                .orderBy(buildOrderSpecifier(sortField, ascending))
                .fetch();
    }

    @Override
    public Optional<TestExecution> findActiveWithGroupById(Long executionId) {
        TestExecution result = queryFactory
                .selectFrom(te)
                .leftJoin(te.testGroup, tg).fetchJoin() // 연관된 그룹 정보를 즉시 로딩
                .where(
                        te.executionId.eq(executionId),
                        te.deletedAt.isNull() // 삭제되지 않은 데이터만 조회
                )
                .fetchOne();

        return Optional.ofNullable(result);
    }

    private OrderSpecifier<?> buildOrderSpecifier(String sortField, boolean ascending) {
        ComparableExpressionBase<?> path = switch (sortField) {
            case "testName" -> te.testName;
            case "status" -> te.status;
            case "durationSeconds" -> te.durationSeconds;
            case "testerName" -> te.testerName;
            case "scenarioSerial" -> te.scenarioSerial;
            case "scenarioAttempt" -> te.scenarioAttempt;
            default -> te.triggeredAt;
        };
        return ascending ? path.asc() : path.desc();
    }
}
