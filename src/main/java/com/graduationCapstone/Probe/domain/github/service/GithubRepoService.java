package com.graduationCapstone.Probe.domain.github.service;

import com.graduationCapstone.Probe.domain.github.dto.GithubRepoResponseDto;
import com.graduationCapstone.Probe.domain.github.dto.GithubRepoSummaryDto;
import com.graduationCapstone.Probe.domain.github.entity.GithubRepo;
import com.graduationCapstone.Probe.domain.github.repository.GithubRepoRepository;
import com.graduationCapstone.Probe.domain.user.entity.User;
import com.graduationCapstone.Probe.global.exception.ErrorCode;
import com.graduationCapstone.Probe.global.exception.handler.CustomException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class GithubRepoService {

    private final GithubRepoRepository githubRepoRepository;

    @Transactional
    public Mono<GithubRepoResponseDto> upsertRepo(User user, GithubRepoSummaryDto dto) {
        return Mono.fromCallable(() -> {
            GithubRepo repo = githubRepoRepository.findByUserIdAndName(user.getId(), dto.name())
                    .map(existing -> {
                        existing.updateFromSummary(dto);
                        return existing;
                    })
                    .orElseGet(() -> GithubRepo.builder()
                            .name(dto.name()).owner(dto.owner()).description(dto.description())
                            .language(dto.language()).forksCount(dto.forksCount())
                            .stargazersCount(dto.stargazersCount()).openIssuesCount(dto.openIssuesCount())
                            .user(user).build());

            GithubRepo saved = githubRepoRepository.save(repo);
            return mapToResponse(saved);
        }).subscribeOn(Schedulers.boundedElastic());
    }

    @Transactional(readOnly = true)
    public Mono<List<GithubRepoResponseDto>> getUserRepos(Long userId) {
        return Mono.fromCallable(() -> githubRepoRepository.findByUserId(userId).stream()
                        .map(this::mapToResponse)
                        .collect(Collectors.toList()))
                .subscribeOn(Schedulers.boundedElastic());
    }

    @Transactional
    public Mono<Void> deleteRepo(Long repoId, Long userId) {
        return Mono.fromRunnable(() -> {
            GithubRepo repo = githubRepoRepository.findById(repoId)
                    .orElseThrow(() -> new CustomException(ErrorCode.RESOURCE_NOT_FOUND));
            if (!repo.getUser().getId().equals(userId)) throw new CustomException(ErrorCode.ACCESS_DENIED);
            githubRepoRepository.delete(repo);
        }).subscribeOn(Schedulers.boundedElastic()).then();
    }

    private GithubRepoResponseDto mapToResponse(GithubRepo repo) {
        return new GithubRepoResponseDto(
                repo.getId(), repo.getName(), repo.getOwner(), repo.getDescription(),
                repo.getLanguage(), repo.getForksCount(), repo.getStargazersCount(), repo.getOpenIssuesCount()
        );
    }
}
