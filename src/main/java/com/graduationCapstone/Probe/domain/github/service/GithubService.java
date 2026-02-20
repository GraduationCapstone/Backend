package com.graduationCapstone.Probe.domain.github.service;

import com.graduationCapstone.Probe.domain.github.dto.GithubFileDto;
import com.graduationCapstone.Probe.domain.github.dto.GithubRepoDto;
import com.graduationCapstone.Probe.domain.github.dto.GithubRepoSummaryDto;
import com.graduationCapstone.Probe.global.exception.ErrorCode;
import com.graduationCapstone.Probe.global.exception.handler.CustomException;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

@Service
public class GithubService {

    private final WebClient webClient;

    /**
     * 생성자 직접 작성: 테스트 코드에서 외부 WebClient 주입을 허용하기 위함입니다.
     * @param webClient 설정된 WebClient 객체
     */
    public GithubService(WebClient webClient) {
        this.webClient = webClient;
    }

    /**
     * 사용자의 GitHub 레포지토리 목록을 description, language, fork 개수, star 개수, issue 개수 정보와 함께 가져옵니다.
     * 최근 업데이트 순으로 정렬하며, Github REST API 기본값에 따라 기본적으로 상위 30개 레포지토리를 조회합니다.
     *
     * @param token GitHub OAuth2 AccessToken
     * @return 레포지토리 요약 정보 리스트를 포함한 Mono
     */
    public Mono<List<GithubRepoSummaryDto>> getRepoList(String token) {
        return webClient.get()
                .uri("/user/repos?sort=updated&per_page=30")
                .headers(h -> h.setBearerAuth(token))
                .retrieve()
                .onStatus(status -> status.equals(HttpStatus.UNAUTHORIZED),
                        response -> Mono.error(new CustomException(ErrorCode.UNAUTHORIZED_ACCESS)))
                .bodyToFlux(new ParameterizedTypeReference<Map<String, Object>>() {})
                .map(repo -> new GithubRepoSummaryDto(
                        (String) repo.get("name"),
                        (String) ((Map<?, ?>) repo.get("owner")).get("login"),
                        (String) repo.get("description"),
                        (String) repo.get("language"),
                        repo.get("forks_count") != null ? (int) repo.get("forks_count") : 0,
                        repo.get("stargazers_count") != null ? (int) repo.get("stargazers_count") : 0,
                        repo.get("open_issues_count") != null ? (int) repo.get("open_issues_count") : 0
                ))
                .collectList()
                .onErrorMap(e -> !(e instanceof CustomException),
                        e -> new CustomException(ErrorCode.INTERNAL_SERVER_ERROR));
    }

    /**
     * 특정 레포지토리의 모든 파일 내용을 재귀적으로 추출합니다.
     * 레포지토리의 기본 브랜치를 먼저 파악한 후, Git Trees API를 통해 전체 파일 구조를 획득합니다.
     *
     * @param owner 레포지토리 소유자 계정 ID
     * @param repo  레포지토리 이름
     * @param token GitHub OAuth2 AccessToken
     * @return 레포지토리 이름과 파일 리스트를 포함한 GithubRepoDto를 담은 Mono
     */
    public Mono<GithubRepoDto> getRepoFullCode(String owner, String repo, String token) {
        return webClient.get()
                .uri("/repos/{owner}/{repo}", owner, repo)
                .headers(h -> h.setBearerAuth(token))
                .retrieve()
                .bodyToMono(new ParameterizedTypeReference<Map<String, Object>>() {
                })
                .flatMap(repoInfo -> {
                    String defaultBranch = (String) repoInfo.get("default_branch");

                    return webClient.get()
                            .uri("/repos/{owner}/{repo}/git/trees/{branch}?recursive=1", owner, repo, defaultBranch)
                            .headers(h -> h.setBearerAuth(token))
                            .retrieve()
                            .bodyToMono(new ParameterizedTypeReference<Map<String, Object>>() {})
                            .flatMap(response -> {
                                Object treeObj = response.get("tree");
                                if (!(treeObj instanceof List)) {
                                    return Mono.empty();
                                }

                                @SuppressWarnings("unchecked")
                                List<Map<String, Object>> tree = (List<Map<String, Object>>) treeObj;

                                return Flux.fromIterable(tree)
                                        .filter(node -> "blob".equals(node.get("type"))) // blob(파일)만 선택
                                        .filter(node -> isSourceCode((String) node.get("path")))
                                        .flatMap(node -> {
                                            String path = (String) node.get("path");
                                            String apiUrl = (String) node.get("url");
                                            return fetchFileContent(apiUrl, path, token);
                                        }, 10)
                                        .collectList()
                                        .map(files -> new GithubRepoDto(repo, files));
                            })
                            .onErrorResume(e -> Mono.empty());
                });
    }

    /**
     * GitHub Blob API를 사용하여 개별 파일의 원본 데이터를 획득합니다.
     * 인코딩 문제를 방지하기 위해 byte array로 수신 후 UTF-8 문자열로 변환합니다.
     * 특정 파일 로드 실패 시 전체 공정을 멈추지 않고 에러 메시지로 대체합니다.
     *
     * @param apiUrl   GitHub Blob API URL
     * @param fileName 파일 경로 및 이름
     * @param token GitHub OAuth2 AccessToken
     * @return 파일 이름과 내용이 포함된 GithubFileDto를 담은 Mono
     */
    private Mono<GithubFileDto> fetchFileContent(String apiUrl, String fileName, String token) {
        return webClient.get()
                .uri(apiUrl)
                .headers(h -> {
                    h.setBearerAuth(token);
                    h.set("Accept", "application/vnd.github.v3.raw");
                })
                .retrieve()
                .bodyToMono(byte[].class)
                .map(bytes -> new GithubFileDto(fileName, new String(bytes, StandardCharsets.UTF_8)))
                .onErrorResume(e -> {
                    // 시스템 로그에만 원인 출력한 뒤 계속 나머지 파일들 불러오도록 함
                    System.err.println("[GitHub API Error] 파일 로드 실패: " + fileName + " | 사유: " + e.getMessage());
                    return Mono.just(new GithubFileDto(fileName, ErrorCode.FILE_NOT_FOUND.getMessage()));
                });
    }

    // 파일 필터링 메소드, 추후 확장자 추가 및 변경
    private boolean isSourceCode(String path) {
        String p = path.toLowerCase();
        return p.endsWith(".html") || p.endsWith(".py") || p.endsWith(".js") || p.endsWith(".java") || p.endsWith(".c");
    }
}
