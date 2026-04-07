package com.graduationCapstone.Probe.domain.test.repository;

import com.graduationCapstone.Probe.domain.test.entity.QTestExecution;
import com.graduationCapstone.Probe.domain.test.entity.TestExecution;
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
