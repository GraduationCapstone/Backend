package com.graduationCapstone.Probe.domain.github.service;

import com.graduationCapstone.Probe.domain.github.dto.GithubRepoSummaryDto;
import com.graduationCapstone.Probe.domain.github.entity.GithubRepo;
import com.graduationCapstone.Probe.domain.github.repository.GithubRepoRepository;
import com.graduationCapstone.Probe.domain.user.entity.User;
import com.graduationCapstone.Probe.global.exception.ErrorCode;
import com.graduationCapstone.Probe.global.exception.handler.CustomException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.test.StepVerifier;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class GithubRepoServiceTest {

    @Mock
    private GithubRepoRepository githubRepoRepository;

    @InjectMocks
    private GithubRepoService githubRepoService;

    @Test
    @DisplayName("레포지토리 저장 시 존재하지 않으면 새로 저장합니다.")
    void insertRepoTest() {
        // given
        User user = User.builder().id(1L).build();
        GithubRepoSummaryDto dto = new GithubRepoSummaryDto("NewRepo", "owner", "description", "Java", 0, 0, 0, true, OffsetDateTime.now());

        when(githubRepoRepository.findByUserIdAndRepoName(1L, "NewRepo")).thenReturn(Optional.empty());

        // save 호출 시 전달된 엔티티에 ID를 부여해서 반환하도록 설정
        when(githubRepoRepository.save(any(GithubRepo.class))).thenAnswer(invocation -> {
            GithubRepo savedRepo = invocation.getArgument(0);
            return GithubRepo.builder()
                    .id(200L) // 신규 생성된 ID 가정
                    .repoName(savedRepo.getRepoName())
                    .isPublic(savedRepo.isPublic())
                    .updatedAt(savedRepo.getUpdatedAt())
                    .user(user)
                    .build();
        });

        // when & then
        githubRepoService.upsertRepo(user, dto)
                .as(StepVerifier::create)
                .expectNextMatches(res -> res.id().equals(200L) && res.repoName().equals("NewRepo") && res.isPublic() == true)
                .verifyComplete();

        // 실제로 save 메서드가 한 번 호출되었는지 검증
        verify(githubRepoRepository, times(1)).save(any());
    }

    @Test
    @DisplayName("레포지토리 저장 시 이미 존재하면 정보를 업데이트합니다.")
    void upsertRepoTest() {
        User user = User.builder().id(1L).build();
        GithubRepoSummaryDto dto = new GithubRepoSummaryDto("Probe", "owner", "desc", "Java", 1, 1, 1, true, OffsetDateTime.now());
        GithubRepo existing = GithubRepo.builder()
                .id(100L)
                .repoName("Probe")
                .isPublic(true)
                .updatedAt(LocalDateTime.now().minusDays(1))
                .user(user)
                .build();

        when(githubRepoRepository.findByUserIdAndRepoName(1L, "Probe")).thenReturn(Optional.of(existing));
        when(githubRepoRepository.save(any())).thenReturn(existing);

        githubRepoService.upsertRepo(user, dto)
                .as(StepVerifier::create)
                .expectNextMatches(res -> res.id().equals(100L))
                .verifyComplete();
    }

    @Test
    @DisplayName("레포지토리 삭제 시 본인 소유가 아니면 ACCESS_DENIED 예외를 발생시킵니다.")
    void deleteRepoAccessDeniedTest() {
        // given
        Long repoId = 1L;
        Long userId = 1L; // 요청자 ID
        Long ownerId = 2L; // 실제 소유자 ID (요청자 ID와 다름)

        User owner = User.builder().id(ownerId).build();
        GithubRepo repo = GithubRepo.builder().id(repoId).user(owner).build();

        when(githubRepoRepository.findById(repoId)).thenReturn(Optional.of(repo));

        // when & then
        githubRepoService.deleteRepo(repoId, userId)
                .as(StepVerifier::create)
                .expectErrorMatches(throwable -> throwable instanceof CustomException
                        && ((CustomException) throwable).getErrorCode() == ErrorCode.ACCESS_DENIED)
                .verify();
    }

    @Test
    @DisplayName("레포지토리 삭제 시 데이터가 존재하고 본인 소유면 정상 삭제됩니다.")
    void deleteRepoSuccessTest() {
        // given
        Long repoId = 1L;
        Long userId = 1L;
        User user = User.builder().id(userId).build();
        GithubRepo repo = GithubRepo.builder().id(repoId).user(user).build();

        when(githubRepoRepository.findById(repoId)).thenReturn(Optional.of(repo));
        doNothing().when(githubRepoRepository).delete(repo);

        // when & then
        githubRepoService.deleteRepo(repoId, userId)
                .as(StepVerifier::create)
                .verifyComplete();

        verify(githubRepoRepository, times(1)).delete(repo);
    }
}