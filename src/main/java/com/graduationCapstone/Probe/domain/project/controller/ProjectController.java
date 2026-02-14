package com.graduationCapstone.Probe.domain.project.controller;

import com.graduationCapstone.Probe.domain.github.dto.GithubRepoResponseDto;
import com.graduationCapstone.Probe.domain.project.dto.ProjectCreateRequestDto;
import com.graduationCapstone.Probe.domain.project.dto.ProjectInviteRequestDto;
import com.graduationCapstone.Probe.domain.project.dto.ProjectNameUpdateRequestDto;
import com.graduationCapstone.Probe.domain.project.dto.ProjectResponseDto;
import com.graduationCapstone.Probe.domain.project.service.ProjectService;
import com.graduationCapstone.Probe.domain.user.entity.User;
import com.graduationCapstone.Probe.global.exception.ErrorCode;
import com.graduationCapstone.Probe.global.exception.handler.CustomException;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

import java.util.List;

@RestController
@RequestMapping("/api/projects")
@RequiredArgsConstructor
@Tag(name = "Project API", description = "프로젝트 및 멤버 관리")
public class ProjectController {

    private final ProjectService projectService;

    @Operation(summary = "프로젝트 생성",
               description = "새로운 프로젝트를 생성하고, 생성자를 멤버로 추가합니다.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "프로젝트 생성 성공"),
            @ApiResponse(responseCode = "401", description = "인증되지 않은 사용자"),
            @ApiResponse(responseCode = "404", description = "사용자 정보를 찾을 수 없음")
    })
    @PostMapping
    public Mono<ResponseEntity<ProjectResponseDto>> createProject(
            @AuthenticationPrincipal User user,
            @RequestBody ProjectCreateRequestDto request) {
        if (user == null) return Mono.error(new CustomException(ErrorCode.UNAUTHORIZED_ACCESS));
        return projectService.createProject(user, request).map(ResponseEntity::ok);
    }

    @Operation(summary = "내 프로젝트 목록 조회",
               description = "자신이 참여 중인 모든 프로젝트를 조회합니다.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "조회 성공"),
            @ApiResponse(responseCode = "401", description = "인증되지 않은 사용자")
    })
    @GetMapping
    public Mono<ResponseEntity<List<ProjectResponseDto>>> getMyProjects(@AuthenticationPrincipal User user) {
        if (user == null) return Mono.error(new CustomException(ErrorCode.UNAUTHORIZED_ACCESS));
        return projectService.getMyProjects(user)
                .collectList()
                .map(ResponseEntity::ok);
    }

    @Operation(summary = "프로젝트 이름 수정",
               description = "프로젝트의 이름을 변경합니다.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "수정 성공"),
            @ApiResponse(responseCode = "404", description = "프로젝트를 찾을 수 없음")
    })
    @PatchMapping("/{projectId}/name")
    public Mono<ResponseEntity<Void>> updateProjectName(
            @PathVariable Long projectId,
            @RequestBody ProjectNameUpdateRequestDto request) {
        return projectService.updateProjectName(projectId, request.projectName())
                .thenReturn(ResponseEntity.ok().build());
    }

    @Operation(summary = "프로젝트 삭제",
               description = "프로젝트를 삭제하고 연관된 멤버 및 레포지토리 연결 정보를 모두 제거합니다.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "삭제 성공 (No Content)"),
            @ApiResponse(responseCode = "404", description = "프로젝트를 찾을 수 없음")
    })
    @DeleteMapping("/{projectId}")
    public Mono<ResponseEntity<Void>> deleteProject(@PathVariable Long projectId) {
        return projectService.deleteProject(projectId)
                .thenReturn(ResponseEntity.noContent().build());
    }

    @Operation(summary = "프로젝트 레포지토리 목록 변경",
               description = "기존 연결을 초기화하고 새로운 레포지토리 목록으로 교체합니다.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "변경 성공"),
            @ApiResponse(responseCode = "404", description = "프로젝트를 찾을 수 없음")
    })
    @PutMapping("/{projectId}/repos")
    public Mono<ResponseEntity<Void>> updateProjectRepos(
            @PathVariable Long projectId,
            @RequestBody List<Long> repoIds) {
        return projectService.updateProjectRepos(projectId, repoIds)
                .thenReturn(ResponseEntity.ok().build());
    }

    @Operation(summary = "프로젝트 멤버 초대",
               description = "여러 사용자를 프로젝트 멤버로 일괄 초대합니다.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "초대 성공"),
            @ApiResponse(responseCode = "404", description = "프로젝트 또는 사용자를 찾을 수 없음")
    })
    @PostMapping("/{projectId}/members")
    public Mono<ResponseEntity<Void>> inviteMember(
            @PathVariable Long projectId,
            @RequestBody ProjectInviteRequestDto request) {
        return projectService.inviteMember(projectId, request.usernames())
                .thenReturn(ResponseEntity.ok().build());
    }

    @Operation(summary = "프로젝트 나가기 (스스로 나가기만 가능)",
               description = "스스로 프로젝트에서 탈퇴합니다. (타인 강퇴 불가)")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "탈퇴 성공"),
            @ApiResponse(responseCode = "403", description = "권한 없음 (남을 강퇴하려고 시도함)"),
            @ApiResponse(responseCode = "404", description = "프로젝트 또는 멤버 정보를 찾을 수 없음")
    })
    @DeleteMapping("/{projectId}/members/{userId}")
    public Mono<ResponseEntity<Void>> removeMember(
            @AuthenticationPrincipal User user,
            @PathVariable Long projectId,
            @PathVariable Long userId) {

        if (!user.getId().equals(userId)) {
            throw new CustomException(ErrorCode.ACCESS_DENIED);
        }

        return projectService.removeMember(projectId, userId)
                .thenReturn(ResponseEntity.noContent().build());
    }

    @Operation(summary = "프로젝트 채널 접속 (레포지토리 조회)",
               description = "프로젝트에 연결된 레포지토리 목록을 조회합니다.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "조회 성공"),
            @ApiResponse(responseCode = "404", description = "프로젝트를 찾을 수 없음")
    })
    @GetMapping("/{projectId}/repos")
    public Mono<ResponseEntity<List<GithubRepoResponseDto>>> getProjectRepos(@PathVariable Long projectId) {
        return projectService.getProjectRepoList(projectId)
                .map(ResponseEntity::ok);
    }
}