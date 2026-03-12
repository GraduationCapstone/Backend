package com.graduationCapstone.Probe.domain.project.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.graduationCapstone.Probe.domain.github.entity.GithubRepo;
import com.graduationCapstone.Probe.domain.github.repository.GithubRepoRepository;
import com.graduationCapstone.Probe.domain.project.dto.*;
import com.graduationCapstone.Probe.domain.project.entity.Project;
import com.graduationCapstone.Probe.domain.project.entity.ProjectMember;
import com.graduationCapstone.Probe.domain.project.entity.ProjectRole;
import com.graduationCapstone.Probe.domain.project.repository.ProjectMemberRepository;
import com.graduationCapstone.Probe.domain.project.repository.ProjectRepoRepository;
import com.graduationCapstone.Probe.domain.project.repository.ProjectRepository;
import com.graduationCapstone.Probe.domain.user.entity.User;
import com.graduationCapstone.Probe.domain.user.repository.UserRepository;
import com.graduationCapstone.Probe.global.exception.ErrorCode;
import com.graduationCapstone.Probe.global.exception.handler.CustomException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.data.redis.core.ReactiveValueOperations;
import org.springframework.test.util.ReflectionTestUtils;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.util.HashSet;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class ProjectServiceTest {

    @InjectMocks
    private ProjectService projectService;

    @Mock
    private ProjectRepository projectRepository;

    @Mock
    private ProjectMemberRepository projectMemberRepository;

    @Mock
    private GithubRepoRepository githubRepoRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private ProjectRepoRepository projectRepoRepository;

    @Mock
    private ReactiveRedisTemplate<String, String> redisTemplate;

    @Mock
    private ReactiveValueOperations<String, String> valueOperations;

    @Mock
    private ObjectMapper objectMapper;

    @Mock
    private EmailService emailService;

    private User testUser;
    private Project testProject;

    @BeforeEach
    void setUp() {
        testUser = User.builder()
                .id(1L)
                .email("test@test.com")
                .username("tester")
                .profileImageUrl("http://tester-image.com")
                .build();
        testProject = Project.builder()
                .id(10L)
                .projectName("Probe Project")
                .members(new HashSet<>())
                .projectRepos(new HashSet<>())
                .build();

        ReflectionTestUtils.setField(projectService, "serverUrl", "http://localhost:8080");

        given(redisTemplate.opsForValue()).willReturn(valueOperations);
        given(valueOperations.delete(anyString())).willReturn(Mono.just(true));

        // 기본적으로 권한 체크 통과하도록 함(필요한 테스트에서만 재정의)
        given(projectMemberRepository.existsByProjectIdAndUserIdAndRole(anyLong(), anyLong(), eq(ProjectRole.OWNER)))
                .willReturn(true);
    }

    @Nested
    @DisplayName("프로젝트 CRUD 테스트")
    class ProjectCrud {
        @Test
        @DisplayName("프로젝트 생성(생성자는 OWNER가 된다)")
        void createProject_Success() {
            String requestName = "New Project";
            ProjectCreateRequestDto request = new ProjectCreateRequestDto(requestName, List.of());

            given(userRepository.findById(anyLong())).willReturn(Optional.of(testUser));
            given(projectRepository.save(any(Project.class))).willReturn(testProject);
            StepVerifier.create(projectService.createProject(testUser, request))
                    .assertNext(res -> assertThat(res).isNotNull())
                    .verifyComplete();

            ArgumentCaptor<Project> projectCaptor = ArgumentCaptor.forClass(Project.class);
            verify(projectRepository, times(1)).save(projectCaptor.capture());

            Project capturedProject = projectCaptor.getValue();
            ProjectMember creator = capturedProject.getMembers().iterator().next();
            assertThat(creator.getRole()).isEqualTo(ProjectRole.OWNER);
        }

        @Test
        @DisplayName("내 프로젝트 목록 조회")
        void getMyProjects_Success() {
            given(projectRepository.findAllByUserId(anyLong())).willReturn(List.of(testProject));
            given(projectMemberRepository.findAllByProjectIdIn(anyList())).willReturn(List.of());
            given(projectRepoRepository.findAllByProjectIdIn(anyList())).willReturn(List.of());

            StepVerifier.create(projectService.getMyProjects(testUser))
                    .expectNextCount(1)
                    .verifyComplete();
        }

        @Test
        @DisplayName("프로젝트 이름 수정(OWNER만 가능)")
        void updateName_Success() {
            given(projectRepository.findById(10L)).willReturn(Optional.of(testProject));
            StepVerifier.create(projectService.updateProjectName(10L, 1L, "New Name")).verifyComplete();
            assertThat(testProject.getProjectName()).isEqualTo("New Name");
        }

        @Test
        @DisplayName("프로젝트 삭제(OWNER만 가능)")
        void delete_Success() {
            given(projectRepository.existsById(10L)).willReturn(true);
            StepVerifier.create(projectService.deleteProject(10L, 1L)).verifyComplete();
            verify(projectRepository).deleteById(10L);
        }

        @Test
        @DisplayName("프로젝트 삭제 실패(OWNER가 아닌 경우)")
        void delete_Fail_NotOwner() {
            // OWNER가 아니라고 가정
            given(projectMemberRepository.existsByProjectIdAndUserIdAndRole(10L, 2L, ProjectRole.OWNER))
                    .willReturn(false);

            StepVerifier.create(projectService.deleteProject(10L, 2L))
                    .expectErrorMatches(e -> e instanceof CustomException &&
                            ((CustomException) e).getErrorCode() == ErrorCode.NOT_PROJECT_OWNER)
                    .verify();
        }
    }

    @Nested
    @DisplayName("프로젝트 초대 및 멤버 관리 테스트")
    class MemberAndInvitation {
        @Test
        @DisplayName("멤버 초대 메일 발송")
        void invite_Success() throws Exception {
            given(projectRepository.findById(anyLong())).willReturn(Optional.of(testProject));
            given(userRepository.existsByEmail(anyString())).willReturn(true);
            given(objectMapper.writeValueAsString(any())).willReturn("json-string");
            given(valueOperations.set(anyString(), any(), any())).willReturn(Mono.just(true));

            StepVerifier.create(projectService.inviteMembers(10L, List.of("invite@test.com")))
                    .verifyComplete();

            verify(emailService, timeout(1000).times(1))
                    .sendInvitationEmail(eq("invite@test.com"), anyString(), eq("Probe Project"));
        }

        @Test
        @DisplayName("초대 수락(초대를 수락한 새로운 멤버는 MEMBER 권한을 가진다")
        void accept_Success() throws Exception {
            InviteTokenInfo info = new InviteTokenInfo(10L, "test@test.com");
            given(valueOperations.get(anyString())).willReturn(Mono.just("json"));
            given(objectMapper.readValue(anyString(), eq(InviteTokenInfo.class))).willReturn(info);
            given(projectRepository.findByIdWithMembers(10L)).willReturn(Optional.of(testProject));
            given(userRepository.findByEmail("test@test.com")).willReturn(Optional.of(testUser));

            StepVerifier.create(projectService.acceptInvitationByToken("token")).verifyComplete();

            // 추가된 멤버의 권한이 MEMBER인지 검증
            ArgumentCaptor<Project> projectCaptor = ArgumentCaptor.forClass(Project.class);
            verify(projectRepository).save(projectCaptor.capture());
            Project capturedProject = projectCaptor.getValue();
            boolean hasMemberRole = capturedProject.getMembers().stream()
                    .anyMatch(m -> m.getRole() == ProjectRole.MEMBER);
            assertThat(hasMemberRole).isTrue();
        }

        @Test
        @DisplayName("토큰 만료")
        void accept_Fail_Token() {
            given(valueOperations.get(anyString())).willReturn(Mono.empty());

            StepVerifier.create(projectService.acceptInvitationByToken("exp"))
                    .expectErrorMatches(e -> e instanceof CustomException &&
                            ((CustomException) e).getErrorCode() == ErrorCode.INVITATION_NOT_FOUND)
                    .verify();
        }

        @Test
        @DisplayName("멤버 삭제(스스로 나가기, 권한이 MEMBER일 경우)")
        void removeMember_Success() {
            ProjectMember member = ProjectMember.builder().role(ProjectRole.MEMBER).build();
            given(projectMemberRepository.findByProjectIdAndUserId(10L, 1L)).willReturn(Optional.of(member));

            StepVerifier.create(projectService.removeMember(10L, 1L)).verifyComplete();
            verify(projectMemberRepository).delete(member);
        }

        @Test
        @DisplayName("멤버 삭제 실패(OWNER는 스스로 나갈 수 없음)")
        void removeMember_Fail_Owner() {
            ProjectMember member = ProjectMember.builder().role(ProjectRole.OWNER).build();
            given(projectMemberRepository.findByProjectIdAndUserId(10L, 1L)).willReturn(Optional.of(member));

            StepVerifier.create(projectService.removeMember(10L, 1L))
                    .expectErrorMatches(e -> e instanceof CustomException &&
                            ((CustomException) e).getErrorCode() == ErrorCode.OWNER_CANNOT_LEAVE)
                    .verify();
        }

        @Test
        @DisplayName("사용자 검색 (나 자신은 제외)")
        void searchUsers_Success() {
            User other = User.builder().id(2L).username("other").email("o@o.com").build();
            given(userRepository.findByUsernameContainingOrEmailContaining(anyString(), anyString()))
                    .willReturn(List.of(testUser, other));

            StepVerifier.create(projectService.searchUsers("keyword", testUser))
                    .assertNext(res -> assertThat(res.username()).isEqualTo("other"))
                    .verifyComplete();
        }

        @Test
        @DisplayName("특정 프로젝트 멤버 목록 조회 시 이미지와 권한이 포함된다")
        void getProjectMembers_Success() {
            // given
            ProjectMember member = ProjectMember.builder()
                    .user(testUser)
                    .role(ProjectRole.OWNER)
                    .build();
            testProject.addMember(member);

            given(projectRepository.findByIdWithMembers(10L)).willReturn(Optional.of(testProject));

            // when & then
            StepVerifier.create(projectService.getProjectMembers(10L))
                    .assertNext(list -> {
                        assertThat(list.get(0).username()).isEqualTo("tester");
                        assertThat(list.get(0).profileImageUrl()).isEqualTo("http://tester-image.com"); // 💡 이미지 확인
                        assertThat(list.get(0).role()).isEqualTo(ProjectRole.OWNER); // 💡 역할 확인
                    })
                    .verifyComplete();
        }
    }

    @Nested
    @DisplayName("프로젝트 레포지토리 관리 테스트")
    class RepoManagement {
        @Test
        @DisplayName("프로젝트 레포지토리 목록 업데이트")
        void updateRepos_Success() {
            given(projectRepository.findByIdWithRepos(10L)).willReturn(Optional.of(testProject));
            GithubRepo repo = GithubRepo.builder().id(50L).build();
            given(githubRepoRepository.findAllById(anyList())).willReturn(List.of(repo));

            StepVerifier.create(projectService.updateProjectRepos(10L, 1L, List.of(50L))).verifyComplete();
            verify(projectRepository).save(testProject);
        }

        @Test
        @DisplayName("프로젝트 레포지토리 목록 조회")
        void getRepoList_Success() {
            given(projectRepository.findByIdWithRepos(10L)).willReturn(Optional.of(testProject));
            StepVerifier.create(projectService.getProjectRepoList(10L))
                    .assertNext(list -> assertThat(list).isNotNull())
                    .verifyComplete();
        }
    }
}