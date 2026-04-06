package com.graduationCapstone.Probe.domain.github.service;

import com.graduationCapstone.Probe.domain.github.dto.GithubRepoResponseDto;
import com.graduationCapstone.Probe.domain.github.dto.GithubRepoSummaryDto;
import com.graduationCapstone.Probe.domain.github.entity.GithubRepo;
import com.graduationCapstone.Probe.domain.github.repository.GithubRepoRepository;
import com.graduationCapstone.Probe.domain.user.entity.User;
import com.graduationCapstone.Probe.global.exception.ErrorCode;
import com.graduationCapstone.Probe.global.exception.handler.CustomException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class GithubRepoService {

    private final GithubRepoRepository githubRepoRepository;

    @Transactional
    public Mono<GithubRepoResponseDto> upsertRepo(User user, GithubRepoSummaryDto dto) {
        log.info("GitHub 레포지토리 정보 저장/업데이트 시도: user={}, repoName={}", user.getUsername(), dto.repoName());
        return Mono.fromCallable(() -> {
            GithubRepo repo = githubRepoRepository.findByUserIdAndRepoName(user.getId(), dto.repoName())
                    .map(existing -> {
                        existing.updateFromSummary(dto);
                        return existing;
                    })
                    .orElseGet(() -> {
                        log.debug("새로운 레포지토리 정보 생성: {}", dto.repoName());
                        return GithubRepo.builder()
                                .repoName(dto.repoName()).owner(dto.owner()).description(dto.description())
                                .language(dto.language()).forksCount(dto.forksCount())
                                .stargazersCount(dto.stargazersCount()).openIssuesCount(dto.openIssuesCount())
                                .isPublic(dto.isPublic()).updatedAt(dto.updatedAt())
                                .user(user).build();
                    });

            GithubRepo saved = githubRepoRepository.save(repo);
            return mapToResponse(saved);
        }).subscribeOn(Schedulers.boundedElastic())
                .doOnSuccess(res -> log.info("레포지토리 정보 저장 완료: id={}", res.id()))
                .doOnError(e -> log.error("레포지토리 정보 저장 실패: {}", e.getMessage()));
    }

    @Transactional(readOnly = true)
    public Mono<List<GithubRepoResponseDto>> getUserRepos(Long userId) {
        log.info("사용자 레포지토리 목록 조회 시도: userId={}", userId);
        return Mono.fromCallable(() -> githubRepoRepository.findByUserId(userId).stream()
                        .map(this::mapToResponse)
                        .collect(Collectors.toList()))
                .subscribeOn(Schedulers.boundedElastic());
    }

    @Transactional
    public Mono<Void> deleteRepo(Long repoId, Long userId) {
        log.info("레포지토리 삭제 시도: repoId={}, userId={}", repoId, userId);
        return Mono.fromRunnable(() -> {
            GithubRepo repo = githubRepoRepository.findById(repoId)
                    .orElseThrow(() -> {
                        log.error("삭제 실패: 리소스를 찾을 수 없음 - repoId={}", repoId);
                        return new CustomException(ErrorCode.RESOURCE_NOT_FOUND);
                    });
            if (!repo.getUser().getId().equals(userId)) {
                log.warn("삭제 거부: 권한 없음 - userId={}, targetRepoOwnerId={}", userId, repo.getUser().getId());
                throw new CustomException(ErrorCode.ACCESS_DENIED);
            }
            githubRepoRepository.delete(repo);
        }).subscribeOn(Schedulers.boundedElastic())
                .doOnSuccess(v -> log.info("레포지토리 삭제 성공: repoId={}", repoId))
                .then();
    }

    private GithubRepoResponseDto mapToResponse(GithubRepo repo) {
        return new GithubRepoResponseDto(
                repo.getId(), repo.getRepoName(), repo.getOwner(), repo.getDescription(),
                repo.getLanguage(), repo.getForksCount(), repo.getStargazersCount(), repo.getOpenIssuesCount(), repo.isPublic(), repo.getUpdatedAt()
        );
    }
}
