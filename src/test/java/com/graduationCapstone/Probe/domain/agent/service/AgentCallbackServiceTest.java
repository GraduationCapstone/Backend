package com.graduationCapstone.Probe.domain.agent.service;

import com.graduationCapstone.Probe.domain.agent.dto.AgentCallbackRequestDto;
import com.graduationCapstone.Probe.domain.test.entity.TestExecution;
import com.graduationCapstone.Probe.domain.test.entity.TestResult;
import com.graduationCapstone.Probe.domain.test.repository.TestExecutionRepository;
import com.graduationCapstone.Probe.domain.test.repository.TestResultRepository;
import com.graduationCapstone.Probe.global.exception.ErrorCode;
import com.graduationCapstone.Probe.global.exception.handler.CustomException;
import org.junit.jupiter.api.DisplayName;
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

    // -------------------------------------------------------
    // 1. 정상 콜백 — COMPLETED + TestResult 저장
    // -------------------------------------------------------
    @Test
    @DisplayName("정상 콜백 수신 시 TestExecution이 COMPLETED로 업데이트되고 TestResult가 저장됨")
    void handleCallback_success_completedWithResults() {
        // given
        Long executionId = 1L;

        TestExecution execution = TestExecution.builder()
                .executionId(executionId)
                .projectId(100L)
                .memberId(1L)
                .status("RUNNING")
                .build();
        given(testExecutionRepository.findById(executionId)).willReturn(Optional.of(execution));

        AgentCallbackRequestDto dto = new AgentCallbackRequestDto(
                executionId,
                "COMPLETED",
                3500L,
                "https://s3.amazonaws.com/bucket/report.zip",
                List.of(
                        new AgentCallbackRequestDto.TestResultDto(
                                "로그인 성공 케이스", "SUCCESS", null, "https://s3.amazonaws.com/bucket/sc1.png"
                        ),
                        new AgentCallbackRequestDto.TestResultDto(
                                "잘못된 비밀번호 케이스", "FAIL", "AssertionError: 예상 오류 메시지와 다름", null
                        )
                )
        );

        // when
        agentCallbackService.handleCallback(dto);

        // then — TestExecution 상태/완료 정보 업데이트 확인
        assertThat(execution.getStatus()).isEqualTo("COMPLETED");
        assertThat(execution.getDurationMs()).isEqualTo(3500L);
        assertThat(execution.getReportS3Url()).isEqualTo("https://s3.amazonaws.com/bucket/report.zip");
        assertThat(execution.getCompletedAt()).isNotNull();
        verify(testExecutionRepository).save(execution);

        // TestResult 저장 확인
        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<TestResult>> captor = ArgumentCaptor.forClass(List.class);
        verify(testResultRepository).saveAll(captor.capture());

        List<TestResult> savedResults = captor.getValue();
        assertThat(savedResults).hasSize(2);
        assertThat(savedResults.get(0).getCaseName()).isEqualTo("로그인 성공 케이스");
        assertThat(savedResults.get(0).getStatus()).isEqualTo("SUCCESS");
        assertThat(savedResults.get(1).getCaseName()).isEqualTo("잘못된 비밀번호 케이스");
        assertThat(savedResults.get(1).getStatus()).isEqualTo("FAIL");
        assertThat(savedResults.get(1).getErrorLog()).isEqualTo("AssertionError: 예상 오류 메시지와 다름");
    }

    // -------------------------------------------------------
    // 2. 정상 콜백 — FAILED, results 없음
    // -------------------------------------------------------
    @Test
    @DisplayName("FAILED 콜백 수신 시 TestExecution 상태가 FAILED로 업데이트되고 TestResult는 저장되지 않음")
    void handleCallback_failed_noResults() {
        // given
        Long executionId = 2L;

        TestExecution execution = TestExecution.builder()
                .executionId(executionId)
                .projectId(200L)
                .memberId(1L)
                .status("RUNNING")
                .build();
        given(testExecutionRepository.findById(executionId)).willReturn(Optional.of(execution));

        AgentCallbackRequestDto dto = new AgentCallbackRequestDto(
                executionId,
                "FAILED",
                1200L,
                null,
                List.of()
        );

        // when
        agentCallbackService.handleCallback(dto);

        // then
        assertThat(execution.getStatus()).isEqualTo("FAILED");
        assertThat(execution.getCompletedAt()).isNotNull();
        verify(testExecutionRepository).save(execution);
        verify(testResultRepository, never()).saveAll(any());
    }

    // -------------------------------------------------------
    // 3. 존재하지 않는 executionId
    // -------------------------------------------------------
    @Test
    @DisplayName("존재하지 않는 executionId로 콜백 시 RESOURCE_NOT_FOUND 예외 발생")
    void handleCallback_executionNotFound_throwsException() {
        // given
        Long nonExistentId = 999L;
        given(testExecutionRepository.findById(nonExistentId)).willReturn(Optional.empty());

        AgentCallbackRequestDto dto = new AgentCallbackRequestDto(
                nonExistentId, "COMPLETED", 1000L, null, List.of()
        );

        // when & then
        assertThatThrownBy(() -> agentCallbackService.handleCallback(dto))
                .isInstanceOf(CustomException.class)
                .extracting(e -> ((CustomException) e).getErrorCode())
                .isEqualTo(ErrorCode.RESOURCE_NOT_FOUND);

        verify(testExecutionRepository, never()).save(any());
        verify(testResultRepository, never()).saveAll(any());
    }
}
