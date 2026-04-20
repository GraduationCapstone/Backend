package com.graduationCapstone.Probe.domain.test.repository;

import com.graduationCapstone.Probe.domain.test.entity.*;
import com.querydsl.core.Tuple;
import com.querydsl.core.types.OrderSpecifier;
import com.querydsl.core.types.dsl.ComparableExpressionBase;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.*;
import java.util.stream.Collectors;

@Repository
@RequiredArgsConstructor
public class TestResultRepositoryImpl implements TestResultRepositoryCustom {

    private final JPAQueryFactory queryFactory;
    private static final QTestResult tr = QTestResult.testResult;
    private static final QTestExecution te = QTestExecution.testExecution;
    private static final QTestGroup tg = QTestGroup.testGroup;

    @Override
    public List<TestResult> findAllActiveByExecutionId(Long executionId, String sortField, boolean ascending) {
        return queryFactory
                .selectFrom(tr)
                .join(tr.testExecution, te).fetchJoin() // 부모 실행 정보도 함께 로드
                .where(
                        tr.testExecution.executionId.eq(executionId),
                        te.deletedAt.isNull(), // 부모가 삭제되었을 시 결과도 노출 X
                        tr.deletedAt.isNull()
                )
                .orderBy(buildOrderSpecifier(sortField, ascending))
                .fetch();
    }

    @Override
    public Optional<TestResult> findActiveById(Long resultId) {
        TestResult result = queryFactory
                .selectFrom(tr)
                .where(
                        tr.resultId.eq(resultId),
                        tr.deletedAt.isNull(),
                        te.deletedAt.isNull() // 부모가 삭제된 경우 조회 X
                )
                .fetchOne();
        return Optional.ofNullable(result);
    }

    @Override
    public Map<ResultStatus, Long> countByStatusForProject(Long projectId) {
        List<Tuple> tuples = queryFactory
                .select(tr.status, tr.count())
                .from(tr)
                .join(te).on(tr.testExecution.executionId.eq(te.executionId))
                .where(
                        te.projectId.eq(projectId),
                        te.deletedAt.isNull(),
                        tr.deletedAt.isNull()
                )
                .groupBy(tr.status)
                .fetch();

        return toStatusMap(tuples);
    }

    @Override
    public Map<ResultStatus, Long> countByStatusForExecution(Long executionId) {
        List<Tuple> tuples = queryFactory
                .select(tr.status, tr.count())
                .from(tr)
                .where(
                        tr.testExecution.executionId.eq(executionId),
                        te.deletedAt.isNull(),
                        tr.deletedAt.isNull()
                )
                .groupBy(tr.status)
                .fetch();

        return toStatusMap(tuples);
    }

    @Override
    public Map<Long, Map<ResultStatus, Long>> findCountsByExecutionIds(List<Long> executionIds) {
        if (executionIds == null || executionIds.isEmpty()) {
            return Collections.emptyMap();
        }

        // executionId와 status별로 그룹화하여 카운트 조회
        List<Tuple> results = queryFactory
                .select(tr.testExecution.executionId, tr.status, tr.count())
                .from(tr)
                .where(
                        tr.testExecution.executionId.in(executionIds),
                        tr.deletedAt.isNull() // Soft Delete 필터링
                )
                .groupBy(tr.testExecution.executionId, tr.status)
                .fetch();

        // 결과를 Map<Long, Map<ResultStatus, Long>> 형태로 변환
        // 예: {1L: {PASS: 10, FAIL: 2}, 2L: {PASS: 5, BLOCK: 1}}
        return results.stream().collect(Collectors.groupingBy(
                t -> t.get(tr.testExecution.executionId),
                Collectors.toMap(
                        t -> t.get(tr.status),
                        t -> t.get(tr.count())
                )
        ));
    }

    private Map<ResultStatus, Long> toStatusMap(List<Tuple> tuples) {
        Map<ResultStatus, Long> map = new EnumMap<>(ResultStatus.class);
        for (ResultStatus s : ResultStatus.values()) {
            map.put(s, 0L);
        }
        for (Tuple tuple : tuples) {
            map.put(tuple.get(tr.status), tuple.get(tr.count()));
        }
        return map;
    }

    private OrderSpecifier<?> buildOrderSpecifier(String sortField, boolean ascending) {
        ComparableExpressionBase<?> path = switch (sortField) {
            case "testCaseNumber" -> tr.testCaseNumber;
            case "testCodeName" -> tr.testCodeName;
            case "status" -> tr.status;
            case "durationSeconds" -> tr.durationSeconds;
            case "caseName" -> tr.caseName;
            default -> tr.createdAt;
        };
        return ascending ? path.asc() : path.desc();
    }
}
