package com.graduationCapstone.Probe.domain.project.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.graduationCapstone.Probe.domain.github.dto.GithubRepoResponseDto;
import com.graduationCapstone.Probe.domain.github.entity.GithubRepo;
import com.graduationCapstone.Probe.domain.github.repository.GithubRepoRepository;
import com.graduationCapstone.Probe.domain.project.dto.*;
import com.graduationCapstone.Probe.domain.project.entity.Project;
import com.graduationCapstone.Probe.domain.project.entity.ProjectMember;
import com.graduationCapstone.Probe.domain.project.entity.ProjectRepo;
import com.graduationCapstone.Probe.domain.project.entity.ProjectRole;
import com.graduationCapstone.Probe.domain.project.repository.ProjectMemberRepository;
import com.graduationCapstone.Probe.domain.project.repository.ProjectRepoRepository;
import com.graduationCapstone.Probe.domain.project.repository.ProjectRepository;
import com.graduationCapstone.Probe.domain.user.entity.User;
import com.graduationCapstone.Probe.domain.user.repository.UserRepository;
import com.graduationCapstone.Probe.global.exception.ErrorCode;
import com.graduationCapstone.Probe.global.exception.handler.CustomException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ProjectService {

    private final ProjectRepository projectRepository;
    private final ProjectMemberRepository projectMemberRepository;
    private final GithubRepoRepository githubRepoRepository;
    private final UserRepository userRepository;
    private final ProjectRepoRepository projectRepoRepository;

    private final ReactiveRedisTemplate<String, String> redisTemplate;
    private final ObjectMapper objectMapper;
    private final EmailService emailService;

    private final TransactionTemplate transactionTemplate;

    @Value("${app.server.url}")
    private String serverUrl;

    /**
     * 새로운 프로젝트를 생성합니다.
     * 프로젝트 생성 시 생성자를 프로젝트 멤버로 등록합니다.
     *
     * @param user    프로젝트를 생성하는 주체 (현재 로그인한 유저)
     * @param request 프로젝트 이름과 초기 레포지토리 ID 목록을 담은 DTO
     * @return 생성된 프로젝트의 상세 정보 (DTO)
     * @throws CustomException USER_NOT_FOUND (유저 정보가 없을 경우)
     */
    public Mono<ProjectResponseDto> createProject(User user, ProjectCreateRequestDto request) {
        log.info("프로젝트 생성 시작: creator={}, projectName={}", user.getUsername(), request.projectName());
        return checkProjectNameDuplicate(user.getId(), request.projectName())
                .flatMap(isDuplicate -> {
                    if (isDuplicate) {
                        return Mono.error(new CustomException(ErrorCode.DUPLICATE_PROJECT_NAME));
                    }

                    return Mono.fromCallable(() -> {
                        ProjectResponseDto response = transactionTemplate.execute(status -> {
                            User persistentUser = userRepository.findById(user.getId())
                                    .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));

                            Project project = Project.builder()
                                    .projectName(request.projectName())
                                    .build();

                            ProjectMember member = ProjectMember.builder()
                                    .user(persistentUser)
                                    .project(project)
                                    .role(ProjectRole.OWNER)
                                    .build();
                            project.addMember(member);

                            if (request.repoIds() != null && !request.repoIds().isEmpty()) {
                                List<GithubRepo> repos = githubRepoRepository.findAllById(request.repoIds());

                                if (repos.size() != request.repoIds().size()) {
                                    throw new CustomException(ErrorCode.REPOSITORY_NOT_FOUND);
                                }

                                for (GithubRepo repo : repos) {
                                    ProjectRepo pr = ProjectRepo.builder()
                                            .githubRepo(repo)
                                            .build();
                                    project.addProjectRepo(pr);
                                }
                            }
                            Project savedProject = projectRepository.save(project);
                            log.info("프로젝트 생성 성공: id={}", savedProject.getId());
                            return ProjectResponseDto.from(savedProject);
                        });
                        return Objects.requireNonNull(response);
                    });
                })
                .subscribeOn(Schedulers.boundedElastic())
                .doOnError(e -> log.error("프로젝트 생성 실패: {}", e.getMessage()));
    }

    /**
     * 프로젝트 이름 중복 여부를 확인합니다.
     * @param userId      사용자 ID
     * @param projectName 확인할 프로젝트 이름
     * @return 중복 여부 (true: 중복)
     */
    public Mono<Boolean> checkProjectNameDuplicate(Long userId, String projectName) {
        return Mono.fromCallable(() ->
                projectRepository.existsByProjectNameAndMembers_User_IdAndMembers_Role(
                        projectName, userId, ProjectRole.OWNER)
        ).subscribeOn(Schedulers.boundedElastic());
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
        log.info("내 프로젝트 목록 조회: userId={}", user.getId());
        return Mono.fromCallable(() -> {
                    List<Project> projects = projectRepository.findAllByUserId(user.getId());
                    log.debug("조회된 프로젝트 수: {}", projects.size());
                    if (projects.isEmpty()) return List.<ProjectResponseDto>of();

                    List<Long> projectIds = projects.stream().map(Project::getId).toList();

                    Map<Long, List<ProjectMember>> memberMap = projectMemberRepository.findAllByProjectIdIn(projectIds)
                            .stream().collect(Collectors.groupingBy(m -> m.getProject().getId()));

                    Map<Long, List<ProjectRepo>> repoMap = projectRepoRepository.findAllByProjectIdIn(projectIds)
                            .stream().collect(Collectors.groupingBy(r -> r.getProject().getId()));

                    return projects.stream().map(project -> {
                        int memberCount = memberMap.getOrDefault(project.getId(), List.of()).size();
                        int repoCount = repoMap.getOrDefault(project.getId(), List.of()).size();

                        return ProjectResponseDto.of(project, memberCount, repoCount);
                    }).toList();
                }).subscribeOn(Schedulers.boundedElastic()).flatMapMany(Flux::fromIterable)
                .doOnError(e -> log.error("프로젝트 목록 조회 중 에러: {}", e.getMessage()));
    }

    /**
     * 프로젝트의 이름을 변경합니다.
     *
     * @param projectId      대상 프로젝트 ID
     * @param newProjectName 변경할 새로운 이름
     * @return Void
     * @throws CustomException PROJECT_NOT_FOUND (프로젝트가 없을 경우)
     */
    public Mono<Void> updateProjectName(Long projectId, Long userId, String newProjectName) {
        log.info("프로젝트명 수정 시도: projectId={}, userId={}, newName={}", projectId, userId, newProjectName);
        return checkProjectNameDuplicate(userId, newProjectName)
                .flatMap(isDuplicate -> {
                    if (isDuplicate) {
                        return Mono.error(new CustomException(ErrorCode.DUPLICATE_PROJECT_NAME));
                    }

                    return Mono.fromRunnable(() -> transactionTemplate.executeWithoutResult(status -> {
                        Project project = projectRepository.findById(projectId)
                                .orElseThrow(() -> new CustomException(ErrorCode.PROJECT_NOT_FOUND));
                        validateOwner(projectId, userId);
                        project.updateProjectName(newProjectName);
                        log.info("프로젝트명 수정 완료: projectId={}", projectId);
                    }));
                })
                .subscribeOn(Schedulers.boundedElastic()).then();
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
    public Mono<Void> deleteProject(Long projectId, Long userId) {
        log.warn("프로젝트 삭제 시도: projectId={}, userId={}", projectId, userId);
        return Mono.fromRunnable(() -> transactionTemplate.executeWithoutResult(status -> {
            if (!projectRepository.existsById(projectId)) {
                throw new CustomException(ErrorCode.PROJECT_NOT_FOUND);
            }
            validateOwner(projectId, userId);
            projectRepository.deleteById(projectId);
            log.info("프로젝트 삭제 완료: projectId={}", projectId);
        })).subscribeOn(Schedulers.boundedElastic()).then();
    }

    /**
     * 키워드(사용자명 또는 이메일)를 통해 사용자를 검색합니다.
     * 현재 로그인한 사용자는 검색 결과에서 제외됩니다.
     *
     * @param keyword     검색할 키워드 (이메일 혹은 아이디의 일부)
     * @param currentUser 현재 검색을 수행 중인 사용자 (결과 제외 대상)
     * @return 검색된 사용자 정보를 담은 리스트
     */
    public Flux<UserSearchResponseDto> searchUsers(String keyword, User currentUser) {
        log.info("사용자 검색 시작: keyword={}, requester={}", keyword, currentUser.getUsername());
        return Mono.fromCallable(() -> {
                    List<User> users = userRepository.findByUsernameContainingIgnoreCase(keyword)
                            .stream()
                            .filter(user -> !user.getId().equals(currentUser.getId()))
                            .toList();

                    if (users.isEmpty()) {
                        log.debug("검색 결과 없음: keyword={}", keyword);
                        return List.<UserSearchResponseDto>of();
                    }

                    return users.stream()
                            .map(UserSearchResponseDto::from)
                            .toList();
                }).subscribeOn(Schedulers.boundedElastic())
                .flatMapMany(Flux::fromIterable)
                .doOnComplete(() -> log.info("사용자 검색 완료: keyword={}", keyword));
    }

    /**
     * 선택한 사용자들의 이메일 주소로 프로젝트 초대 링크를 일괄 발송합니다.
     *
     * @param projectId 초대할 대상 프로젝트의 고유 ID
     * @param emails    초대장을 보낼 이메일 주소 리스트
     * @return 비동기 처리가 완료됨을 나타내는 {@link Mono}
     * @throws CustomException ErrorCode.PROJECT_NOT_FOUND - 프로젝트가 존재하지 않을 경우 발생
     */
    public Mono<Void> inviteMembers(Long projectId, List<String> emails) {
        log.info("프로젝트 멤버 초대 시작: projectId={}, targetEmails={}", projectId, emails);
        return Mono.fromCallable(() -> {
                    Project project = transactionTemplate.execute(status ->
                            projectRepository.findById(projectId)
                                    .orElseThrow(() -> new CustomException(ErrorCode.PROJECT_NOT_FOUND))
                    );
                    return java.util.Objects.requireNonNull(project);
                })
                .subscribeOn(Schedulers.boundedElastic())
                .flatMap(project -> {
                    String projectName = project.getProjectName();

                    return Flux.fromIterable(emails)
                            .flatMap(email -> {
                                log.debug("초대 메일 발송 큐 등록: {}", email);
                                return sendInviteToSingleUser(projectId, email, projectName)
                                        .onErrorResume(e -> {
                                            log.error("초대 실패: email={}, error={}", email, e.getMessage());
                                            return Mono.empty();
                                        });
                            })
                            .then();
                });
    }

    /**
     * 단일 유저에게 초대 토큰을 생성하고 메일을 발송합니다. (내부 로직)
     * 토큰 정보는 Redis에 24시간 동안 저장됩니다.
     *
     * @param projectId   초대할 프로젝트 ID
     * @param email       대상 이메일 주소
     * @param projectName 이메일 본문에 표시될 프로젝트 이름
     * @return 비동기 처리가 완료됨을 나타내는 {@link Mono}
     */
    private Mono<Void> sendInviteToSingleUser(Long projectId, String email, String projectName) {
        return Mono.fromCallable(() -> userRepository.existsByEmail(email))
                .subscribeOn(Schedulers.boundedElastic())
                .flatMap(exists -> {
                    if (!exists) return Mono.empty();

                    String token = UUID.randomUUID().toString();
                    String redisKey = "invite:" + token;
                    InviteTokenInfo info = new InviteTokenInfo(projectId, email);

                    try {
                        String jsonValue = objectMapper.writeValueAsString(info);
                        String link = serverUrl + "/api/projects/accept?token=" + token;

                        return redisTemplate.opsForValue()
                                .set(redisKey, jsonValue, Duration.ofHours(24))
                                .then(Mono.fromRunnable(() -> emailService.sendInvitationEmail(email, link, projectName)).subscribeOn(Schedulers.boundedElastic()));

                    } catch (JsonProcessingException e) {
                        return Mono.<Void>error(new CustomException(ErrorCode.INTERNAL_SERVER_ERROR));
                    }
                })
                .then();
    }

    /**
     * 이메일 링크를 통해 전달된 토큰을 검증하고 사용자를 프로젝트 멤버로 등록합니다.
     * 검증 성공 시 사용된 토큰은 Redis에서 즉시 삭제됩니다.
     *
     * @param token 이메일에 포함된 초대 인증 토큰
     * @return 비동기 처리가 완료됨을 나타내는 {@link Mono}
     * @throws CustomException ErrorCode.INVITATION_NOT_FOUND - 토큰이 만료되었거나 존재하지 않을 경우
     * @throws CustomException ErrorCode.INTERNAL_SERVER_ERROR - 데이터 파싱 중 오류 발생 시
     */
    public Mono<Void> acceptInvitationByToken(String token) {
        log.info("초대 수락 시도: tokenKey=invite:{}", token);
        String redisKey = "invite:" + token;
        return redisTemplate.opsForValue().get(redisKey)
                .switchIfEmpty(Mono.defer(() -> {
                    log.warn("초대 수락 실패: 존재하지 않거나 만료된 토큰");
                    return Mono.error(new CustomException(ErrorCode.INVITATION_NOT_FOUND));
                }))
                .flatMap(jsonValue -> {
                    try {
                        InviteTokenInfo info = objectMapper.readValue(jsonValue, InviteTokenInfo.class);
                        log.debug("토큰 파싱 성공: projectId={}, email={}", info.projectId(), info.inviteEmail());
                        return saveMemberToDb(info.projectId(), info.inviteEmail());
                    } catch (JsonProcessingException e) {
                        log.error("초대 토큰 파싱 중 오류 발생: {}", e.getMessage(), e);
                        return Mono.error(new CustomException(ErrorCode.INTERNAL_SERVER_ERROR));
                    }
                })
                .then(redisTemplate.opsForValue().delete(redisKey))
                .doOnSuccess(v -> log.info("초대 수락 및 멤버 등록 완료"))
                .then();
    }

    /**
     * 사용자 정보를 확인한 후 DB에 프로젝트 멤버로 저장합니다. (내부 로직)
     * 이미 멤버인 경우 중복 저장을 방지합니다.
     *
     * @param projectId 프로젝트 고유 ID
     * @param email     멤버로 등록할 사용자의 이메일
     * @return 비동기 처리가 완료됨을 나타내는 {@link Mono}
     */
    private Mono<Void> saveMemberToDb(Long projectId, String email) {
        String lockKey = "lock:project:join:" + projectId + ":" + email; //락의 키를 프로젝트ID와 이메일 조합으로 생성

        return redisTemplate.opsForValue()
                // 락 획득 시도 (10초 타임아웃으로 데드락 방지)
                .setIfAbsent(lockKey, "locked", Duration.ofSeconds(10))
                .flatMap(isLocked -> {
                    if (Boolean.FALSE.equals(isLocked)) {
                        // 락 획득 실패 시: 이미 다른 요청 처리 중
                        log.warn("동일한 초대 수락 요청이 처리 중입니다: email={}", email);
                        return Mono.empty();
                    }

                    Mono<Void> businessLogic = Mono.fromRunnable(() -> transactionTemplate.executeWithoutResult(status -> {
                                Project project = projectRepository.findByIdWithMembers(projectId)
                                        .orElseThrow(() -> new CustomException(ErrorCode.PROJECT_NOT_FOUND));

                                User user = userRepository.findByEmail(email)
                                        .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));

                                boolean isAlreadyMember = project.getMembers().stream()
                                        .anyMatch(m -> m.getUser().getId().equals(user.getId()));

                                if (isAlreadyMember) {
                                    log.info("이미 멤버인 사용자: projectId={}, email={}", projectId, email);
                                } else {
                                    ProjectMember newMember = ProjectMember.builder()
                                            .user(user)
                                            .project(project)
                                            .role(ProjectRole.MEMBER)
                                            .build();
                                    project.addMember(newMember);
                                    projectRepository.save(project);
                                    log.info("신규 멤버 등록 성공: projectId={}, email={}", projectId, email);
                                }
                            })).subscribeOn(Schedulers.boundedElastic()).then();

                            return businessLogic
                                .doFinally(signalType ->
                                            redisTemplate.opsForValue().delete(lockKey).subscribe()
                            );
                })
                .then();
    }

    /**
     * 특정 프로젝트에 속한 모든 멤버 목록을 조회합니다.
     *
     * @param projectId 조회할 프로젝트 ID
     * @return 멤버 정보 리스트를 담은 {@link Mono}
     * @throws CustomException ErrorCode.PROJECT_NOT_FOUND - 프로젝트가 존재하지 않을 경우
     */
    public Mono<List<ProjectMemberResponseDto>> getProjectMembers(Long projectId) {
        log.info("멤버 목록 조회: projectId={}", projectId);
        return Mono.fromCallable(() -> {
            List<ProjectMemberResponseDto> members = transactionTemplate.execute(status -> {
                Project project = projectRepository.findByIdWithMembers(projectId)
                        .orElseThrow(() -> new CustomException(ErrorCode.PROJECT_NOT_FOUND));

                return project.getMembers().stream()
                        .map(ProjectMemberResponseDto::from)
                        .toList();
            });
            return Objects.requireNonNull(members);
        }).subscribeOn(Schedulers.boundedElastic())
                .doOnSuccess(list -> log.info("멤버 목록 조회 완료: projectId={}, count={}", projectId, list.size()));
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
    public Mono<Void> removeMember(Long projectId, Long userId) {
        log.info("멤버 프로젝트 탈퇴 시도: projectId={}, userId={}", projectId, userId);
        return Mono.fromRunnable(() -> transactionTemplate.executeWithoutResult(status -> {
            ProjectMember member = projectMemberRepository.findByProjectIdAndUserId(projectId, userId)
                    .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));

            if (member.getRole() == ProjectRole.OWNER) {
                throw new CustomException(ErrorCode.OWNER_CANNOT_LEAVE);
            }
            projectMemberRepository.delete(member);
        }))
                .subscribeOn(Schedulers.boundedElastic())
                .doOnSuccess(v -> log.info("멤버 탈퇴 완료: userId={}", userId))
                .then();
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
    public Mono<Void> updateProjectRepos(Long projectId, Long userId, List<Long> newRepoIds) {
        log.info("레포지토리 목록 수정 시작: projectId={}, userId={}, newRepoIds={}", projectId, userId, newRepoIds);
        return Mono.fromRunnable(() -> transactionTemplate.executeWithoutResult(status -> {
            Project project = projectRepository.findByIdWithRepos(projectId)
                    .orElseThrow(() -> new CustomException(ErrorCode.PROJECT_NOT_FOUND));
            validateOwner(projectId, userId);

            project.getProjectRepos().clear();

            if (newRepoIds != null && !newRepoIds.isEmpty()) {
                List<GithubRepo> repos = githubRepoRepository.findAllById(newRepoIds);

                if (repos.size() != newRepoIds.size()) {
                    log.error("레포 수정 실패: 일부 레포를 찾을 수 없음 - 요청 IDs: {}", newRepoIds);
                    throw new CustomException(ErrorCode.REPOSITORY_NOT_FOUND);
                }

                for (GithubRepo repo : repos) {
                    ProjectRepo pr = ProjectRepo.builder()
                            .githubRepo(repo)
                            .build();
                    project.addProjectRepo(pr);
                }
            }
            projectRepository.save(project);
            log.info("레포지토리 목록 수정 완료: projectId={}", projectId);
        })).subscribeOn(Schedulers.boundedElastic()).then();
    }

    /**
     * 프로젝트에 연결된 GitHub 레포지토리 목록을 조회합니다.
     *
     * @param projectId 대상 프로젝트 ID
     * @return 연결된 레포지토리 정보 리스트
     */
    public Mono<List<GithubRepoResponseDto>> getProjectRepoList(Long projectId) {
        log.info("프로젝트 레포 목록 조회: projectId={}", projectId);
        return Mono.fromCallable(() -> {
            List<GithubRepoResponseDto> repos = transactionTemplate.execute(status -> {
                Project project = projectRepository.findByIdWithRepos(projectId)
                        .orElseThrow(() -> new CustomException(ErrorCode.PROJECT_NOT_FOUND));

                return project.getProjectRepos().stream()
                        .map(ProjectRepo::getGithubRepo)
                        .map(GithubRepoResponseDto::from)
                        .collect(Collectors.toList());
            });
            return Objects.requireNonNull(repos);
        }).subscribeOn(Schedulers.boundedElastic())
                .doOnSuccess(list -> log.info("레포 목록 조회 완료: projectId={}, count={}", projectId, list.size()));
    }

    private void validateOwner(Long projectId, Long userId) {
        boolean isOwner = projectMemberRepository.existsByProjectIdAndUserIdAndRole(
                projectId, userId, ProjectRole.OWNER);

        if (!isOwner) {
            log.warn("권한 검증 실패: 소유자 아님 - projectId={}, userId={}", projectId, userId);
            throw new CustomException(ErrorCode.NOT_PROJECT_OWNER);
        }
    }
}