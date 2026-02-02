package com.graduationCapstone.Probe.domain.github.controller;

import com.graduationCapstone.Probe.domain.github.dto.GithubRepoDto;
import com.graduationCapstone.Probe.domain.github.dto.GithubRepoSummaryDto;
import com.graduationCapstone.Probe.domain.github.service.GithubService;
import com.graduationCapstone.Probe.domain.user.entity.User;
import com.graduationCapstone.Probe.domain.user.repository.UserRepository;
import com.graduationCapstone.Probe.global.exception.ErrorCode;
import com.graduationCapstone.Probe.global.exception.handler.CustomException;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.util.List;

@RestController
@RequestMapping("/api/github")
@RequiredArgsConstructor
@Tag(name = "GitHub API", description = "GitHub 레포지토리 정보 조회 및 파일 분석 API")
public class GithubController {

    private final GithubService githubService;
    private final UserRepository userRepository;

    /**
     * 인증된 사용자의 GitHub 레포지토리 목록을 조회합니다.
     * 비동기 스트림 내에서 사용자의 최신 액세스 토큰을 확인하여 GitHub API로부터 레포지토리 요약 정보를 가져옵니다.
     *
     * @param user 인증된 사용자 정보 (@AuthenticationPrincipal)
     * @return 사용자의 레포지토리 요약 정보 리스트를 포함한 Mono (200 OK)
     */
    @Operation(
            summary = "레포지토리 목록 조회",
            description = "로그인된 사용자의 모든 GitHub 레포지토리 목록을 가져옵니다. 사용자가 테스트할 레포지토리를 선택할 수 있도록 요약된 정보를 반환합니다.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "조회 성공"),
            @ApiResponse(responseCode = "401", description = "인증 실패"),
            @ApiResponse(responseCode = "500", description = "서버 내부 에러")
    })
    @GetMapping("/repos/list")
    public Mono<ResponseEntity<List<GithubRepoSummaryDto>>> getRepoList(@AuthenticationPrincipal User user) {
        if (user == null) {
            return Mono.error(new CustomException(ErrorCode.UNAUTHORIZED_ACCESS));
        }

        final Long userId = user.getId();

        return Mono.defer(() -> Mono.fromCallable(() -> userRepository.findById(userId)
                                .map(User::getGithubAccessToken)
                                .orElseThrow(() -> new CustomException(ErrorCode.UNAUTHORIZED_ACCESS)))
                    .subscribeOn(Schedulers.boundedElastic())
                    .flatMap(githubService::getRepoList)
                    .map(ResponseEntity::ok)
        ).onErrorResume(e -> {
            if (e instanceof CustomException) return Mono.error(e);
            return Mono.error(new CustomException(ErrorCode.INTERNAL_SERVER_ERROR));
        });
    }

    /**
     * 사용자가 선택한 특정 레포지토리의 모든 소스 코드를 추출합니다.
     * 해당 레포지토리의 전체 디렉토리를 재귀적으로 탐색하며, 각 파일의 원문 코드를 추출하여 반환합니다.
     *
     * @param user     인증된 사용자 정보 (@AuthenticationPrincipal)
     * @param repoName 추출 대상 레포지토리 이름
     * @return 선택된 레포지토리 정보와 파일 리스트를 포함한 Mono (200 OK)
     */
    @Operation(
            summary = "선택된 레포지토리 전체 코드 추출",
            description = "사용자가 선택한 특정 레포지토리의 모든 소스 코드와 파일 내용을 재귀적으로 탐색하여 가져옵니다.")
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "코드 추출 성공",
                    content = @Content(schema = @Schema(implementation = GithubRepoDto.class))
            ),
            @ApiResponse(responseCode = "401", description = "인증 실패"),
            @ApiResponse(responseCode = "404", description = "레포지토리를 찾을 수 없음"),
            @ApiResponse(responseCode = "500", description = "추출 중 서버 에러")
    })
    @GetMapping("/repos/{repoName}/code")
    public Mono<ResponseEntity<GithubRepoDto>> getFullCode(
            @AuthenticationPrincipal User user,
            @Parameter(description = "추출할 레포지토리 이름", example = "Probe")
            @PathVariable String repoName) {

        if (user == null) return Mono.error(new CustomException(ErrorCode.UNAUTHORIZED_ACCESS));

        final Long userId = user.getId();

        return Mono.defer(() -> Mono.fromCallable(() -> userRepository.findById(userId)
                        .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND)))
                .subscribeOn(Schedulers.boundedElastic())
                .flatMap(freshUser -> {
                    String token = freshUser.getGithubAccessToken();
                    if (token == null) return Mono.error(new CustomException(ErrorCode.UNAUTHORIZED_ACCESS));

                    return githubService.getRepoFullCode(freshUser.getUsername(), repoName, token);
                })
                .map(repoData -> {
                    if (repoData == null) throw new CustomException(ErrorCode.REPOSITORY_NOT_FOUND);
                    return ResponseEntity.ok(repoData);
                })
        ).onErrorResume(e -> {
            if (e instanceof CustomException) return Mono.error(e);
            return Mono.error(new CustomException(ErrorCode.INTERNAL_SERVER_ERROR));
        });
    }
}
