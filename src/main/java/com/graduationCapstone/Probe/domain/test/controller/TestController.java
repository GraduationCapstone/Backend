package com.graduationCapstone.Probe.domain.test.controller;

import com.graduationCapstone.Probe.domain.test.dto.*;
import com.graduationCapstone.Probe.domain.test.entity.*;
import com.graduationCapstone.Probe.domain.test.repository.*;
import com.graduationCapstone.Probe.domain.test.service.TestService;
import com.graduationCapstone.Probe.domain.user.entity.User;
import com.graduationCapstone.Probe.global.exception.ErrorCode;
import com.graduationCapstone.Probe.global.exception.handler.CustomException;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.List;

@Tag(name = "Test", description = "테스트 대시보드 및 결과 데이터 관리 API")
@RestController
@RequestMapping("/api/projects/{projectId}/tests")
@RequiredArgsConstructor
public class TestController {

    private final TestService testService;
    private final TestGroupRepository groupRepo;
    private final TestExecutionRepository executionRepo;
    private final TestResultRepository resultRepo;

    // ── 초기 설정 및 중복 체크 ──

    @Operation(summary = "테스트 그룹 엔티티 생성", description = "테스트 그룹 및 실행 데이터를 생성합니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "생성 성공"),
            @ApiResponse(responseCode = "400", description = "잘못된 요청 데이터")
    })
    @PostMapping("/setup")
    public ResponseEntity<Void> setup(
            @PathVariable Long projectId,
            @AuthenticationPrincipal User user,
            @Valid @RequestBody TestGroupCreateRequestDto dto) {
        testService.createTestEntities(user, projectId, dto);
        return ResponseEntity.ok().build();
    }

    @Operation(summary = "테스트 그룹명 중복 체크", description = "프로젝트 내에 동일한 테스트 그룹명이 존재하는지 확인합니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "중복 여부 반환 성공")})
    @GetMapping("/check-duplicate")
    public ResponseEntity<Boolean> checkDuplicate(
            @PathVariable Long projectId,
            @RequestParam String name) {
        return ResponseEntity.ok(testService.isGroupNameDuplicate(projectId, name));
    }

    // ── 수정 및 삭제 (Management) ──

    @Operation(summary = "테스트 그룹명 수정", description = "프로젝트 소유권 확인 후 테스트 그룹명을 업데이트합니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "수정 성공"),
            @ApiResponse(responseCode = "400", description = "유효하지 않은 입력값"),
            @ApiResponse(responseCode = "403", description = "해당 프로젝트에 속하지 않은 테스트 그룹"),
            @ApiResponse(responseCode = "404", description = "테스트 그룹 정보를 찾을 수 없음")
    })
    @PatchMapping("/groups/{groupId}")
    public ResponseEntity<Void> updateGroupName(
            @PathVariable Long projectId,
            @PathVariable Long groupId,
            @Valid @RequestBody TestGroupNameUpdateDto dto) {
        testService.updateGroupName(projectId, groupId, dto.newGroupName());
        return ResponseEntity.ok().build();
    }

    @Operation(summary = "테스트 그룹 삭제", description = "특정 테스트 그룹을 삭제합니다.")
    @ApiResponses(
            {@ApiResponse(responseCode = "204", description = "삭제 성공"), @ApiResponse(responseCode = "404", description = "그룹 없음")})
    @DeleteMapping("/groups/{groupId}")
    public ResponseEntity<Void> deleteGroup(
            @PathVariable Long projectId,
            @PathVariable Long groupId) {
        testService.deleteGroup(projectId, groupId);
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "테스트 코드명 수정", description = "개별 테스트 코드의 이름을 변경합니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "수정 성공"),
            @ApiResponse(responseCode = "403", description = "프로젝트 소속 불일치"),
            @ApiResponse(responseCode = "404", description = "결과 정보를 찾을 수 없음")
    })
    @PatchMapping("/results/{resultId}/name")
    public ResponseEntity<Void> updateTestCodeName(
            @PathVariable Long projectId,
            @PathVariable Long resultId,
            @Valid @RequestBody TestCodeNameUpdateDto dto) {
        TestResult r = resultRepo.findActiveById(resultId)
                .orElseThrow(() -> new CustomException(ErrorCode.RESOURCE_NOT_FOUND));

        if (!r.getTestExecution().getProjectId().equals(projectId)) {
            throw new CustomException(ErrorCode.ACCESS_DENIED);
        }

        r.updateTestCodeName(dto.newTestCodeName());
        resultRepo.save(r);
        return ResponseEntity.ok().build();
    }

    @Operation(summary = "테스트 코드 삭제 (Soft Delete)")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "삭제 성공"),
            @ApiResponse(responseCode = "403", description = "프로젝트 소속 불일치"),
            @ApiResponse(responseCode = "404", description = "결과 정보를 찾을 수 없음")
    })
    @DeleteMapping("/results/{resultId}")
    public ResponseEntity<Void> deleteResult(
            @PathVariable Long projectId,
            @PathVariable Long resultId) {
        TestResult r = resultRepo.findActiveById(resultId)
                .orElseThrow(() -> new CustomException(ErrorCode.RESOURCE_NOT_FOUND));

        if (!r.getTestExecution().getProjectId().equals(projectId)) {
            throw new CustomException(ErrorCode.ACCESS_DENIED);
        }

        r.softDelete();
        if (r.isDeleted()) resultRepo.save(r);
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "테스트 실행 내역 삭제 (Soft Delete)")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "삭제 성공"),
            @ApiResponse(responseCode = "403", description = "프로젝트 소속 불일치"),
            @ApiResponse(responseCode = "404", description = "실행 정보를 찾을 수 없음")
    })
    @DeleteMapping("/executions/{executionId}")
    public ResponseEntity<Void> deleteExecution(
            @PathVariable Long projectId,
            @PathVariable Long executionId) {
        TestExecution e = executionRepo.findActiveById(executionId)
                .orElseThrow(() -> new CustomException(ErrorCode.RESOURCE_NOT_FOUND));

        if (!e.getProjectId().equals(projectId)) {
            throw new CustomException(ErrorCode.ACCESS_DENIED);
        }

        e.softDelete();
        if (e.isDeleted()) executionRepo.save(e);
        return ResponseEntity.noContent().build();
    }



    // ── 목록 및 정렬 (Listings) ──

    @Operation(summary = "테스트 그룹 조회", description = "특정 테스트 그룹의 상세 정보를 조회합니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "조회 성공"),
            @ApiResponse(responseCode = "404", description = "그룹 정보를 찾을 수 없음")
    })
    @GetMapping("/groups/{groupId}")
    public ResponseEntity<TestGroupResponseDto> getGroup(
            @PathVariable Long projectId,
            @PathVariable Long groupId) {
        TestGroup g = groupRepo.findById(groupId)
                .orElseThrow(() -> new CustomException(ErrorCode.RESOURCE_NOT_FOUND));

        if (!g.getProjectId().equals(projectId)) {
            throw new CustomException(ErrorCode.ACCESS_DENIED);
        }

        return ResponseEntity.ok(TestGroupResponseDto.from(g));
    }

    @Operation(summary = "테스트 그룹 내(ID, 테스트코드명, 상태, 테스트 시간, 테스터, 일시) 조회 및 정렬 (필드 선택 가능)", description = "field를 입력하지 않으면 기본값(id)으로 정렬됩니다.")
    @GetMapping("/list/basic") // 쿼리 파라미터 사용
    public ResponseEntity<List<TestExecutionListDto>> getBasicListOptional(
            @PathVariable Long projectId,
            @RequestParam(required = false, defaultValue = "id") String field,
            @RequestParam(defaultValue = "false") boolean asc) {
        return ResponseEntity.ok(testService.getBasicSortedList(projectId, field, asc));
    }

    @Operation(summary = "프로젝트 내 테스트 그룹 정보(ID, 테스트명, Pass 비율, 테스트 시간, 테스터, 일시) 조회 및 정렬 (필드 선택 가능)",
            description = "field 미입력 시 기본값(id)으로 정렬됩니다.")
    @ApiResponses({@ApiResponse(responseCode = "200", description = "요약 목록 조회 성공")})
    @GetMapping("/list/summary") // 쿼리 파라미터 사용
    public ResponseEntity<List<TestCaseSummaryDto>> getSummaryList(
            @PathVariable Long projectId,
            @RequestParam(required = false, defaultValue = "id") String field,
            @RequestParam(defaultValue = "false") boolean asc) {
        return ResponseEntity.ok(testService.getTestSummaryList(projectId, field, asc));
    }

    @Operation(summary = "테스트 그룹 내 레포지토리 검색", description = "특정 테스트 그룹의 targetRepoId 등 정보를 조회합니다.")
    @GetMapping("/groups/{groupId}/repo")
    public ResponseEntity<Long> getGroupRepo(@PathVariable Long projectId, @PathVariable Long groupId) {
        TestGroup g = groupRepo.findById(groupId)
                .orElseThrow(() -> new CustomException(ErrorCode.RESOURCE_NOT_FOUND));

        if (!g.getProjectId().equals(projectId)) {
            throw new CustomException(ErrorCode.ACCESS_DENIED);
        }
        return ResponseEntity.ok(g.getTargetRepoId());
    }

    // ── 상세 조회 및 다운로드 (Details & Downloads) ──

    @Operation(summary = "테스트 계획서 다운로드", description = "S3에 저장된 테스트 계획서(.xlsx) URL로 리다이렉트합니다.")
    @ApiResponses({@ApiResponse(responseCode = "302", description = "다운로드 URL로 이동")})
    @GetMapping("/executions/{executionId}/download/plan")
    public ResponseEntity<Void> downloadPlan(@PathVariable Long projectId, @PathVariable Long executionId) {
        TestExecution e = executionRepo.findActiveById(executionId)
                .orElseThrow(() -> new CustomException(ErrorCode.RESOURCE_NOT_FOUND));

        if (!e.getProjectId().equals(projectId)) {
            throw new CustomException(ErrorCode.ACCESS_DENIED);
        }

        if (e.getPlanS3Url() == null || e.getPlanS3Url().isBlank()) {
            throw new CustomException(ErrorCode.RESOURCE_NOT_FOUND);
        }
        return ResponseEntity.status(302).location(URI.create(e.getPlanS3Url())).build();
    }

    @Operation(summary = "테스트 보고서 다운로드", description = "S3에 저장된 결과 보고서(.xlsx) URL로 리다이렉트합니다.")
    @ApiResponses({@ApiResponse(responseCode = "302", description = "다운로드 URL로 이동")})
    @GetMapping("/executions/{executionId}/download/report")
    public ResponseEntity<Void> downloadReport(@PathVariable Long projectId, @PathVariable Long executionId) {
        TestExecution e = executionRepo.findActiveById(executionId)
                .orElseThrow(() -> new CustomException(ErrorCode.RESOURCE_NOT_FOUND));

        if (!e.getProjectId().equals(projectId)) {
            throw new CustomException(ErrorCode.ACCESS_DENIED);
        }

        if (e.getPlanResultS3Url() == null || e.getPlanResultS3Url().isBlank()) {
            throw new CustomException(ErrorCode.RESOURCE_NOT_FOUND);
        }
        return ResponseEntity.status(302).location(URI.create(e.getPlanResultS3Url())).build();
    }

    @Operation(summary = "테스트 결과 중 테스트 시나리오 항목 조회", description = "대시보드에서 테스트 결과 상세 조회 시 '테스트 시나리오'을 눌렀을 때 떠야하는 항목들")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "상세 조회 성공"),
            @ApiResponse(responseCode = "404", description = "활성화된 결과가 없거나 찾을 수 없음")
    })
    @GetMapping("/results/{resultId}/view-full")
    public ResponseEntity<TestResultDetailFullDto> getFullView(
            @PathVariable Long projectId,
            @PathVariable Long resultId) {
        TestResult r = resultRepo.findActiveById(resultId).orElseThrow(() -> new CustomException(ErrorCode.RESOURCE_NOT_FOUND));
        if (!r.getTestExecution().getProjectId().equals(projectId)) throw new CustomException(ErrorCode.INVALID_ARGUMENT);

        ScenarioDetailData d = r.getScenarioDetail();
        return ResponseEntity.ok(new TestResultDetailFullDto(
                r.getTestExecution().getTesterName(), r.getStatus().name(), r.getTestExecution().getCompletedAt(),
                d.scenarioName(), d.description(), d.testCaseId(), d.testCaseName(),
                d.precondition(), d.testData(), d.executionSteps(), d.result()
        ));
    }

    @Operation(summary = "테스트 결과 중 결과, 테스트 코드, 증명 항목 조회", description = "대시보드에서 테스트 결과 상세 조회 시 type(basic -> 결과, code -> 테스트 코드, visual -> 증명)에 따라 다른 상세 정보를 반환합니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "상세 정보 반환 성공"),
            @ApiResponse(responseCode = "400", description = "유효하지 않은 type 파라미터"),
            @ApiResponse(responseCode = "404", description = "결과 정보를 찾을 수 없음")
    })
    @GetMapping("/results/{resultId}/details")
    public ResponseEntity<Object> getDetails(
            @PathVariable Long projectId,
            @PathVariable Long resultId,
            @RequestParam String type) {
        TestResult r = resultRepo.findActiveById(resultId)
                .orElseThrow(() -> new CustomException(ErrorCode.RESOURCE_NOT_FOUND));

        if (!r.getTestExecution().getProjectId().equals(projectId)) {
            throw new CustomException(ErrorCode.ACCESS_DENIED);
        }

        return switch (type) {
            case "visual" -> ResponseEntity.ok(new TestResultDetailVisualDto(r.getStatus().name(), r.getScreenshotS3Urls()));
            case "code" -> ResponseEntity.ok(new TestResultDetailCodeDto(r.getTestExecution().getTesterName(), r.getStatus().name(), r.getCreatedAt(), r.getTestCode()));
            case "basic" -> ResponseEntity.ok(new TestResultDetailBasicDto(r.getTestExecution().getTesterName(), r.getStatus().name(), r.getCreatedAt(), r.getErrorLog()));
            default -> ResponseEntity.badRequest().build();
        };
    }

    // ── 통계 및 개수 (Stats & Counts) ──

    @Operation(summary = "특정 프로젝트 내 테스트 케이스 총 개수", description = "특정 프로젝트 내에서 테스트 케이스의 총 개수를 조회합니다.")
    @ApiResponses({@ApiResponse(responseCode = "200", description = "개수 조회 성공")})
    @GetMapping("/count/total")
    public ResponseEntity<Long> getTotalCount(@PathVariable Long projectId) {
        return ResponseEntity.ok(testService.countTotalExecutions(projectId));
    }

    @Operation(summary = "날짜별 평균 테스트 시간", description = "일자별 평균 소요 시간을 조회합니다.")
    @ApiResponses({@ApiResponse(responseCode = "200", description = "통계 조회 성공")})
    @GetMapping("/stats/daily-avg")
    public ResponseEntity<List<DailyAvgDurationResponseDto>> getDailyAvg(@PathVariable Long projectId) {
        return ResponseEntity.ok(testService.getDailyAvgDurations(projectId));
    }

    @Operation(summary = "프로젝트 전체 통계 조회", description = "프로젝트 내 모든 테스트 그룹의 Pass/Block/Fail/Untest 개수 및 Pass 비율을 조회합니다.")
    @ApiResponses({@ApiResponse(responseCode = "200", description = "전체 통계 조회 성공")})
    @GetMapping("/stats/project-global")
    public ResponseEntity<PassRateResponseDto> getGlobalStats(@PathVariable Long projectId) {
        return ResponseEntity.ok(testService.getProjectGlobalStats(projectId));
    }

    @Operation(summary = "테스트 케이스 통계 조회", description = "테스트 케이스 내의 Pass/Block/Fail/Untest 개수 및 Pass 비율을 조회합니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "실행 통계 조회 성공"),
            @ApiResponse(responseCode = "404", description = "실행 정보를 찾을 수 없음")
    })
    @GetMapping("/{executionId}/stats")
    public ResponseEntity<PassRateResponseDto> getStats(
            @PathVariable Long projectId,
            @PathVariable Long executionId) {
        TestExecution exec = executionRepo.findById(executionId).orElseThrow(() -> new CustomException(ErrorCode.RESOURCE_NOT_FOUND));
        if (!exec.getProjectId().equals(projectId)) throw new CustomException(ErrorCode.INVALID_ARGUMENT);

        return ResponseEntity.ok(testService.getPassRateStats(executionId));
    }

    @Operation(summary = "특정 테스트 그룹 내 테스트 케이스 총 개수", description = "테스트 그룹 하위에 속한 모든 테스트 케이스 개수를 조회합니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "개수 조회 성공"),
            @ApiResponse(responseCode = "404", description = "그룹 정보를 찾을 수 없음")
    })
    @GetMapping("/groups/{groupId}/count/results")
    public ResponseEntity<Long> getCount(
            @PathVariable Long projectId,
            @PathVariable Long groupId) {
        TestGroup group = groupRepo.findById(groupId).orElseThrow(() -> new CustomException(ErrorCode.RESOURCE_NOT_FOUND));
        if (!group.getProjectId().equals(projectId)) throw new CustomException(ErrorCode.INVALID_ARGUMENT);
        return ResponseEntity.ok(testService.countResultsInGroup(groupId));
    }
}