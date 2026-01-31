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
     * 클라이언트 UI에서 테스트할 레포지토리를 선택할 수 있도록 요약된 정보를 반환합니다.
     *
     * @param user 인증된 사용자 정보 (@AuthenticationPrincipal)
     * @return 사용자의 레포지토리 요약 정보 리스트 (200 OK)
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
    public ResponseEntity<List<GithubRepoSummaryDto>> getRepoList(@AuthenticationPrincipal User user) {
        String token = getValidToken(user);
        if (token == null) {
            throw new CustomException(ErrorCode.UNAUTHORIZED_ACCESS);
        }
        try {
            List<GithubRepoSummaryDto> repos = githubService.getRepoList(token).block();
            return ResponseEntity.ok(repos);
        } catch (Exception e) {
            throw new CustomException(ErrorCode.INTERNAL_SERVER_ERROR);
        }
    }

    /**
     * 사용자가 선택한 특정 레포지토리의 모든 소스 코드를 추출합니다.
     * 해당 레포지토리의 전체 디렉토리를 재귀적으로 탐색하여 파일명과 파일 내용을 가져옵니다.
     *
     * @param user     인증된 사용자 정보
     * @param repoName 테스트를 위해 선택한 레포지토리 이름
     * @return 선택된 레포지토리 이름과 파일 리스트를 포함한 GithubRepoDto (200 OK)
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
    public ResponseEntity<GithubRepoDto> getFullCode(
            @AuthenticationPrincipal User user,
            @Parameter(description = "추출할 레포지토리 이름", example = "Probe")
            @PathVariable String repoName) {

        String token = getValidToken(user);
        if (token == null) {
            throw new CustomException(ErrorCode.UNAUTHORIZED_ACCESS);
        }

        try {
            User freshUser = userRepository.findById(user.getId())
                    .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));
            GithubRepoDto repoData = githubService.getRepoFullCode(freshUser.getUsername(), repoName, token).block();

            if (repoData == null) {
                throw new CustomException(ErrorCode.REPOSITORY_NOT_FOUND);
            }

            return ResponseEntity.ok(repoData);
        } catch (CustomException e) {
            throw e;
        } catch (Exception e) {
            throw new CustomException(ErrorCode.INTERNAL_SERVER_ERROR);
        }
    }

    /**
     * 사용자의 유효한 GitHub AccessToken을 데이터베이스에서 조회합니다.
     *
     * @param user 인증된 사용자 정보
     * @return 유효한 액세스 토큰 문자열, 없을 경우 null 반환
     */
    private String getValidToken(User user) {
        return userRepository.findById(user.getId())
                .map(User::getGithubAccessToken)
                .orElse(null);
    }
}