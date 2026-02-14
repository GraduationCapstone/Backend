package com.graduationCapstone.Probe.domain.project.service;

import com.graduationCapstone.Probe.domain.github.dto.GithubRepoResponseDto;
import com.graduationCapstone.Probe.domain.github.entity.GithubRepo;
import com.graduationCapstone.Probe.domain.github.repository.GithubRepoRepository;
import com.graduationCapstone.Probe.domain.project.dto.ProjectCreateRequestDto;
import com.graduationCapstone.Probe.domain.project.dto.ProjectResponseDto;
import com.graduationCapstone.Probe.domain.project.entity.Project;
import com.graduationCapstone.Probe.domain.project.entity.ProjectMember;
import com.graduationCapstone.Probe.domain.project.entity.ProjectRepo;
import com.graduationCapstone.Probe.domain.project.repository.ProjectMemberRepository;
import com.graduationCapstone.Probe.domain.project.repository.ProjectRepository;
import com.graduationCapstone.Probe.domain.user.entity.User;
import com.graduationCapstone.Probe.domain.user.repository.UserRepository;
import com.graduationCapstone.Probe.global.exception.ErrorCode;
import com.graduationCapstone.Probe.global.exception.handler.CustomException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ProjectService {

    private final ProjectRepository projectRepository;
    private final ProjectMemberRepository projectMemberRepository;
    private final GithubRepoRepository githubRepoRepository;
    private final UserRepository userRepository;

    /**
     * 새로운 프로젝트를 생성합니다.
     * 프로젝트 생성 시 생성자를 프로젝트 멤버로 등록합니다.
     *
     * @param user    프로젝트를 생성하는 주체 (현재 로그인한 유저)
     * @param request 프로젝트 이름과 초기 레포지토리 ID 목록을 담은 DTO
     * @return 생성된 프로젝트의 상세 정보 (DTO)
     * @throws CustomException USER_NOT_FOUND (유저 정보가 없을 경우)
     */
    @Transactional
    public Mono<ProjectResponseDto> createProject(User user, ProjectCreateRequestDto request) {
        return Mono.fromCallable(() -> {
            User persistentUser = userRepository.findById(user.getId())
                    .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));

            Project project = Project.builder()
                    .projectName(request.projectName())
                    .build();
            projectRepository.save(project);

            ProjectMember member = ProjectMember.builder()
                    .project(project)
                    .user(persistentUser)
                    .build();
            projectMemberRepository.save(member);

            if (request.repoIds() != null && !request.repoIds().isEmpty()) {
                List<GithubRepo> repos = githubRepoRepository.findAllById(request.repoIds());
                for (GithubRepo repo : repos) {
                    project.getProjectRepos().add(ProjectRepo.builder()
                            .project(project)
                            .githubRepo(repo)
                            .build());
                }
            }
            projectRepository.save(project);
            return ProjectResponseDto.from(project);
        }).subscribeOn(Schedulers.boundedElastic());
    }

    /**
     * 내가 속한 모든 프로젝트 목록을 조회합니다.
     * <p>
     * `findAllByUserId`(Fetch Join 적용)를 사용하여,
     * 프로젝트와 연관된 멤버 및 레포지토리 정보를 한 번에 가져옵니다.
     * (N+1 문제 및 LazyInitializationException 방지)
     * </p>
     *
     * @param user 조회를 요청한 사용자
     * @return 프로젝트 목록
     */
    public Flux<ProjectResponseDto> getMyProjects(User user) {
        return Mono.fromCallable(() -> {
                    List<Project> projects = projectRepository.findAllByUserId(user.getId());

                    return projects.stream()
                            .map(ProjectResponseDto::from)
                            .toList();
                }).subscribeOn(Schedulers.boundedElastic())
                .flatMapMany(Flux::fromIterable);
    }

    /**
     * 프로젝트의 이름을 변경합니다.
     *
     * @param projectId      대상 프로젝트 ID
     * @param newProjectName 변경할 새로운 이름
     * @return Void
     * @throws CustomException PROJECT_NOT_FOUND (프로젝트가 없을 경우)
     */
    @Transactional
    public Mono<Void> updateProjectName(Long projectId, String newProjectName) {
        return Mono.fromRunnable(() -> {
            Project project = projectRepository.findById(projectId)
                    .orElseThrow(() -> new CustomException(ErrorCode.PROJECT_NOT_FOUND));
            project.updateProjectName(newProjectName);
        }).subscribeOn(Schedulers.boundedElastic()).then();
    }

    /**
     * 프로젝트를 삭제합니다.
     * <p>
     * Cascade 설정에 의해 프로젝트가 삭제되면, 소속 멤버 및 레포지토리 연결 정보도 함께 삭제됩니다.
     * </p>
     *
     * @param projectId 삭제할 프로젝트 ID
     * @return Void
     * @throws CustomException PROJECT_NOT_FOUND (프로젝트가 없을 경우)
     */
    @Transactional
    public Mono<Void> deleteProject(Long projectId) {
        return Mono.fromRunnable(() -> {
            if (!projectRepository.existsById(projectId)) {
                throw new CustomException(ErrorCode.PROJECT_NOT_FOUND);
            }
            projectRepository.deleteById(projectId);
        }).subscribeOn(Schedulers.boundedElastic()).then();
    }

    /**
     * 프로젝트에 여러 멤버를 일괄 초대합니다.
     * <p>
     * 이미 프로젝트에 속해있는 유저는 메모리 상에서 필터링하여 제외합니다.
     * </p>
     *
     * @param projectId 대상 프로젝트 ID
     * @param usernames 초대할 사용자들의 GitHub 아이디(username) 목록
     * @return Void
     * @throws CustomException PROJECT_NOT_FOUND, USER_NOT_FOUND
     */
    @Transactional
    public Mono<Void> inviteMember(Long projectId, List<String> usernames) {
        return Mono.fromRunnable(() -> {
            Project project = projectRepository.findByIdWithMembers(projectId)
                    .orElseThrow(() -> new CustomException(ErrorCode.PROJECT_NOT_FOUND));

            List<User> usersToInvite = userRepository.findAllByUsernameIn(usernames);

            if (usersToInvite.isEmpty()) {
                throw new CustomException(ErrorCode.USER_NOT_FOUND);
            }

            List<ProjectMember> newMembers = new ArrayList<>();

            for (User user : usersToInvite) {
                boolean isAlreadyMember = project.getMembers().stream()
                        .anyMatch(m -> m.getUser().getId().equals(user.getId()));

                if (!isAlreadyMember) {
                    newMembers.add(ProjectMember.builder()
                            .project(project)
                            .user(user)
                            .build());
                }
            }
            projectMemberRepository.saveAll(newMembers);
        }).subscribeOn(Schedulers.boundedElastic()).then();
    }

    /**
     * 프로젝트에서 특정 멤버를 삭제합니다.
     * <p>
     * 현재 정책상 회원이 <b>스스로 프로젝트를 탈퇴할 때</b> 사용됩니다.
     * (Controller 계층에서 본인 확인 후 호출됨)
     * </p>
     *
     * @param projectId 대상 프로젝트 ID
     * @param userId    삭제할 멤버(본인)의 고유 ID (PK)
     * @return Void
     * @throws CustomException USER_NOT_FOUND (해당 멤버가 프로젝트에 없는 경우)
     */
    @Transactional
    public Mono<Void> removeMember(Long projectId, Long userId) {
        return Mono.fromRunnable(() -> {
            ProjectMember member = projectMemberRepository.findByProjectIdAndUserId(projectId, userId)
                    .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));
            projectMemberRepository.delete(member);
        }).subscribeOn(Schedulers.boundedElastic()).then();
    }

    /**
     * 프로젝트에 연결된 레포지토리 목록을 수정합니다.
     * 기존 연결을 모두 해제(Clear)하고, 요청된 새 레포지토리 목록으로 교체합니다.
     *
     * @param projectId  대상 프로젝트 ID
     * @param newRepoIds 새로 설정할 레포지토리 ID 목록
     * @return Void
     * @throws CustomException PROJECT_NOT_FOUND
     */
    @Transactional
    public Mono<Void> updateProjectRepos(Long projectId, List<Long> newRepoIds) {
        return Mono.fromRunnable(() -> {
            Project project = projectRepository.findByIdWithRepos(projectId)
                    .orElseThrow(() -> new CustomException(ErrorCode.PROJECT_NOT_FOUND));

            project.getProjectRepos().clear();

            if (newRepoIds != null && !newRepoIds.isEmpty()) {
                List<GithubRepo> repos = githubRepoRepository.findAllById(newRepoIds);
                for (GithubRepo repo : repos) {
                    project.getProjectRepos().add(ProjectRepo.builder()
                            .project(project)
                            .githubRepo(repo)
                            .build());
                }
            }
            projectRepository.save(project);
        }).subscribeOn(Schedulers.boundedElastic()).then();
    }

    /**
     * 프로젝트에 연결된 GitHub 레포지토리 목록을 조회합니다.
     *
     * @param projectId 대상 프로젝트 ID
     * @return 연결된 레포지토리 정보 리스트
     */
    public Mono<List<GithubRepoResponseDto>> getProjectRepoList(Long projectId) {
        return Mono.fromCallable(() -> {
            Project project = projectRepository.findByIdWithRepos(projectId)
                    .orElseThrow(() -> new CustomException(ErrorCode.PROJECT_NOT_FOUND));

            return project.getProjectRepos().stream()
                    .map(ProjectRepo::getGithubRepo)
                    .map(GithubRepoResponseDto::from)
                    .collect(Collectors.toList());
        }).subscribeOn(Schedulers.boundedElastic());
    }
}