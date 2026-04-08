package com.graduationCapstone.Probe.domain.agent.service;

import com.graduationCapstone.Probe.domain.agent.dto.AgentPlanCallbackRequestDto;
import com.graduationCapstone.Probe.domain.agent.dto.AgentTestCallbackRequestDto;
import com.graduationCapstone.Probe.domain.test.entity.*;
import com.graduationCapstone.Probe.domain.test.repository.TestExecutionRepository;
import com.graduationCapstone.Probe.domain.test.repository.TestResultRepository;
import com.graduationCapstone.Probe.domain.user.entity.User;
import com.graduationCapstone.Probe.global.exception.ErrorCode;
import com.graduationCapstone.Probe.global.exception.handler.CustomException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class AgentCallbackServiceTest {

    @InjectMocks
    private AgentCallbackService agentCallbackService;

    @Mock
    private TestExecutionRepository testExecutionRepository;

    @Mock
    private TestResultRepository testResultRepository;

    private TestExecution createTestExecution(Long executionId, ExecutionStatus status) {
        User tester = User.builder()
                .id(1L)
                .githubId("12345")
                .username("tester")
                .email("tester@test.com")
                .build();

        return TestExecution.builder()
                .executionId(executionId)
                .projectId(100L)
                .tester(tester)
                .testerName("tester")
                .scenarioSerial("01")
                .scenarioAttempt(1)
                .testName("회원가입")
                .status(status)
                .build();
    }

    // =======================================================
    // 1단계 콜백: handlePlanCallback
    // =======================================================
    @Nested
    @DisplayName("1단계 콜백 - handlePlanCallback")
    class PlanCallbackTest {

        @Test
        @DisplayName("정상 계획서 콜백 수신 시 상태가 PLAN_COMPLETED로 변경되고 planS3Url이 저장됨")
        void handlePlanCallback_success() {
            // given
            Long executionId = 1L;
            TestExecution execution = createTestExecution(executionId, ExecutionStatus.PLAN_GENERATING);
            given(testExecutionRepository.findById(executionId)).willReturn(Optional.of(execution));

            AgentPlanCallbackRequestDto dto = new AgentPlanCallbackRequestDto(
                    executionId,
                    "https://s3.amazonaws.com/bucket/plan.xlsx"
            );

            // when
            agentCallbackService.handlePlanCallback(dto);

            // then
            assertThat(execution.getStatus()).isEqualTo(ExecutionStatus.PLAN_COMPLETED);
            assertThat(execution.getPlanS3Url()).isEqualTo("https://s3.amazonaws.com/bucket/plan.xlsx");
            verify(testExecutionRepository).save(execution);
        }

        @Test
        @DisplayName("존재하지 않는 executionId로 계획서 콜백 시 RESOURCE_NOT_FOUND 예외 발생")
        void handlePlanCallback_executionNotFound() {
            // given
            Long nonExistentId = 999L;
            given(testExecutionRepository.findById(nonExistentId)).willReturn(Optional.empty());

            AgentPlanCallbackRequestDto dto = new AgentPlanCallbackRequestDto(
                    nonExistentId, "https://s3.amazonaws.com/bucket/plan.xlsx"
            );

            // when & then
            assertThatThrownBy(() -> agentCallbackService.handlePlanCallback(dto))
                    .isInstanceOf(CustomException.class)
                    .extracting(e -> ((CustomException) e).getErrorCode())
                    .isEqualTo(ErrorCode.RESOURCE_NOT_FOUND);

            verify(testExecutionRepository, never()).save(any());
        }
    }

    // =======================================================
    // 2단계 콜백: handleTestCallback
    // =======================================================
    @Nested
    @DisplayName("2단계 콜백 - handleTestCallback")
    class TestCallbackTest {

        @Test
        @DisplayName("정상 테스트 콜백 수신 시 COMPLETED로 업데이트되고 TestResult가 저장됨")
        void handleTestCallback_success_completedWithResults() {
            // given
            Long executionId = 1L;
            TestExecution execution = createTestExecution(executionId, ExecutionStatus.TESTING);
            given(testExecutionRepository.findById(executionId)).willReturn(Optional.of(execution));

            ScenarioDetailData scenarioDetail = new ScenarioDetailData(
                    "회원가입", "회원가입 기능 테스트", "T0101_01", "정상 회원가입",
                    "미가입 사용자", "valid@email.com", "1. 회원가입 페이지 이동 2. 정보 입력", "가입 성공"
            );

            AgentTestCallbackRequestDto dto = new AgentTestCallbackRequestDto(
                    executionId,
                    ExecutionStatus.COMPLETED,
                    35.5,
                    "https://s3.amazonaws.com/bucket/plan_result.xlsx",
                    "https://s3.amazonaws.com/bucket/test.spec.js",
                    List.of(
                            new AgentTestCallbackRequestDto.TestResultDto(
                                    "01", "정상 회원가입 케이스", "TC_SIGNUP_01",
                                    ResultStatus.PASS, 12.3, null,
                                    "test('회원가입', async () => { ... })",
                                    scenarioDetail,
                                    List.of("https://s3.amazonaws.com/bucket/sc1.png")
                            ),
                            new AgentTestCallbackRequestDto.TestResultDto(
                                    "02", "중복 이메일 케이스", "TC_SIGNUP_02",
                                    ResultStatus.FAIL, 8.7, "AssertionError: 예상 오류 메시지와 다름",
                                    "test('중복 이메일', async () => { ... })",
                                    null,
                                    List.of()
                            )
                    )
            );

            // when
            agentCallbackService.handleTestCallback(dto);

            // then — TestExecution 상태/완료 정보 업데이트 확인
            assertThat(execution.getStatus()).isEqualTo(ExecutionStatus.COMPLETED);
            assertThat(execution.getDurationSeconds()).isEqualTo(35.5);
            assertThat(execution.getPlanResultS3Url()).isEqualTo("https://s3.amazonaws.com/bucket/plan_result.xlsx");
            assertThat(execution.getTestSpecS3Url()).isEqualTo("https://s3.amazonaws.com/bucket/test.spec.js");
            assertThat(execution.getCompletedAt()).isNotNull();
            verify(testExecutionRepository).save(execution);

            // TestResult 저장 확인
            @SuppressWarnings("unchecked")
            ArgumentCaptor<List<TestResult>> captor = ArgumentCaptor.forClass(List.class);
            verify(testResultRepository).saveAll(captor.capture());

            List<TestResult> savedResults = captor.getValue();
            assertThat(savedResults).hasSize(2);

            TestResult first = savedResults.get(0);
            assertThat(first.getTestCaseNumber()).isEqualTo("01");
            assertThat(first.getCaseName()).isEqualTo("정상 회원가입 케이스");
            assertThat(first.getTestCodeName()).isEqualTo("TC_SIGNUP_01");
            assertThat(first.getStatus()).isEqualTo(ResultStatus.PASS);
            assertThat(first.getDurationSeconds()).isEqualTo(12.3);
            assertThat(first.getTestCode()).contains("회원가입");
            assertThat(first.getScenarioDetail()).isNotNull();
            assertThat(first.getScenarioDetail().scenarioName()).isEqualTo("회원가입");
            assertThat(first.getScreenshotS3Urls()).containsExactly("https://s3.amazonaws.com/bucket/sc1.png");

            TestResult second = savedResults.get(1);
            assertThat(second.getTestCaseNumber()).isEqualTo("02");
            assertThat(second.getStatus()).isEqualTo(ResultStatus.FAIL);
            assertThat(second.getErrorLog()).isEqualTo("AssertionError: 예상 오류 메시지와 다름");
        }

        @Test
        @DisplayName("FAILED 콜백 수신 시 상태가 FAILED로 업데이트되고 TestResult는 저장되지 않음")
        void handleTestCallback_failed_noResults() {
            // given
            Long executionId = 2L;
            TestExecution execution = createTestExecution(executionId, ExecutionStatus.TESTING);
            given(testExecutionRepository.findById(executionId)).willReturn(Optional.of(execution));

            AgentTestCallbackRequestDto dto = new AgentTestCallbackRequestDto(
                    executionId,
                    ExecutionStatus.FAILED,
                    null,
                    null, null,
                    List.of()
            );

            // when
            agentCallbackService.handleTestCallback(dto);

            // then
            assertThat(execution.getStatus()).isEqualTo(ExecutionStatus.FAILED);
            assertThat(execution.getCompletedAt()).isNotNull();
            verify(testExecutionRepository).save(execution);
            verify(testResultRepository, never()).saveAll(any());
        }

        @Test
        @DisplayName("존재하지 않는 executionId로 테스트 콜백 시 RESOURCE_NOT_FOUND 예외 발생")
        void handleTestCallback_executionNotFound() {
            // given
            Long nonExistentId = 999L;
            given(testExecutionRepository.findById(nonExistentId)).willReturn(Optional.empty());

            AgentTestCallbackRequestDto dto = new AgentTestCallbackRequestDto(
                    nonExistentId, ExecutionStatus.COMPLETED, 10.0, null, null, List.of()
            );

            // when & then
            assertThatThrownBy(() -> agentCallbackService.handleTestCallback(dto))
                    .isInstanceOf(CustomException.class)
                    .extracting(e -> ((CustomException) e).getErrorCode())
                    .isEqualTo(ErrorCode.RESOURCE_NOT_FOUND);

            verify(testExecutionRepository, never()).save(any());
            verify(testResultRepository, never()).saveAll(any());
        }
    }
}
