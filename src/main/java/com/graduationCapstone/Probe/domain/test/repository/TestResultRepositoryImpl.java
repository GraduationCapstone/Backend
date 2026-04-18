package com.graduationCapstone.Probe.domain.test.repository;

import com.graduationCapstone.Probe.domain.test.entity.QTestExecution;
import com.graduationCapstone.Probe.domain.test.entity.QTestResult;
import com.graduationCapstone.Probe.domain.test.entity.ResultStatus;
import com.graduationCapstone.Probe.domain.test.entity.TestResult;
import com.querydsl.core.Tuple;
import com.querydsl.core.types.OrderSpecifier;
import com.querydsl.core.types.dsl.ComparableExpressionBase;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class TestResultRepositoryImpl implements TestResultRepositoryCustom {

    private final JPAQueryFactory queryFactory;
    private static final QTestResult tr = QTestResult.testResult;
    private static final QTestExecution te = QTestExecution.testExecution;

    @Override
    public List<TestResult> findAllActiveByExecutionId(Long executionId, String sortField, boolean ascending) {
        return queryFactory
                .selectFrom(tr)
                .where(
                        tr.testExecution.executionId.eq(executionId),
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
                        tr.deletedAt.isNull()
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
                        tr.deletedAt.isNull()
                )
                .groupBy(tr.status)
                .fetch();

        return toStatusMap(tuples);
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
