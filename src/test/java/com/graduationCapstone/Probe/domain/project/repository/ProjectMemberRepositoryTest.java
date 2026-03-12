package com.graduationCapstone.Probe.domain.project.repository;

import com.graduationCapstone.Probe.domain.project.entity.Project;
import com.graduationCapstone.Probe.domain.project.entity.ProjectMember;
import com.graduationCapstone.Probe.domain.project.entity.ProjectRole;
import com.graduationCapstone.Probe.domain.user.entity.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
class ProjectMemberRepositoryTest {

    @Autowired
    private ProjectMemberRepository projectMemberRepository;

    @Autowired
    private TestEntityManager em;

    private User user;
    private Project project;

    @BeforeEach
    void setUp() {
        user = User.builder()
                .username("tester")
                .email("test@test.com")
                .githubId("1234")
                .build();
        em.persist(user);

        project = Project.builder()
                .projectName("Test Project")
                .build();
        em.persist(project);

        ProjectMember member = ProjectMember.builder()
                .project(project)
                .user(user)
                .role(ProjectRole.OWNER)
                .build();
        em.persist(member);

        em.flush();
        em.clear();
    }

    @Test
    @DisplayName("OWNER 권한 확인 쿼리 테스트")
    void existsByProjectIdAndUserIdAndRole_Test() {
        // Given (setUp에서 이미 OWNER로 저장)

        // When
        boolean isOwner = projectMemberRepository.existsByProjectIdAndUserIdAndRole(
                project.getId(), user.getId(), ProjectRole.OWNER);

        boolean isMember = projectMemberRepository.existsByProjectIdAndUserIdAndRole(
                project.getId(), user.getId(), ProjectRole.MEMBER);

        // Then
        assertThat(isOwner).isTrue();  // OWNER이므로 true
        assertThat(isMember).isFalse(); // MEMBER가 아니므로 false
    }

    @Test
    @DisplayName("프로젝트 ID와 유저 ID로 멤버 조회 (FETCH JOIN 확인)")
    void findByProjectIdAndUserId_Test() {
        // When
        Optional<ProjectMember> result = projectMemberRepository.findByProjectIdAndUserId(
                project.getId(), user.getId());

        // Then
        assertThat(result).isPresent();
        assertThat(result.get().getUser().getUsername()).isEqualTo("tester");
        assertThat(result.get().getRole()).isEqualTo(ProjectRole.OWNER);
    }

    @Test
    @DisplayName("존재하지 않는 유저나 프로젝트 조회 시 false 반환")
    void exists_Fail_Test() {
        // When
        boolean invalidUser = projectMemberRepository.existsByProjectIdAndUserIdAndRole(
                project.getId(), 999L, ProjectRole.OWNER);

        boolean invalidProject = projectMemberRepository.existsByProjectIdAndUserIdAndRole(
                999L, user.getId(), ProjectRole.OWNER);

        // Then
        assertThat(invalidUser).isFalse();
        assertThat(invalidProject).isFalse();
    }
}