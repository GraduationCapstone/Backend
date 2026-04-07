package com.graduationCapstone.Probe.domain.project.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.graduationCapstone.Probe.domain.github.dto.GithubRepoResponseDto;
import com.graduationCapstone.Probe.domain.project.dto.*;
import com.graduationCapstone.Probe.domain.project.entity.ProjectRole;
import com.graduationCapstone.Probe.domain.project.service.ProjectService;
import com.graduationCapstone.Probe.domain.user.entity.User;
import com.graduationCapstone.Probe.domain.user.repository.UserRepository;
import com.graduationCapstone.Probe.global.security.jwt.util.JwtUtil;
import com.graduationCapstone.Probe.global.security.util.CookieUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.thymeleaf.spring6.SpringTemplateEngine;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.OffsetDateTime;
import java.util.Collections;
import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.given;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(ProjectController.class)
class ProjectControllerTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;

    @MockitoBean private ProjectService projectService;
    @MockitoBean private SpringTemplateEngine templateEngine;
    @MockitoBean private JwtUtil jwtUtil;
    @MockitoBean private UserRepository userRepository;
    @MockitoBean private CookieUtil cookieUtil;

    private User loginUser;
    private Authentication auth;

    @BeforeEach
    void setUp() {
        loginUser = User.builder().id(1L).username("testUser").email("test@test.com").build();
        auth = new UsernamePasswordAuthenticationToken(loginUser, null,
                Collections.singletonList(new SimpleGrantedAuthority("ROLE_USER")));
    }

    @Nested
    @DisplayName("프로젝트 기본 관리 (CRUD)")
    class ProjectBasic {
        @Test
        @DisplayName("프로젝트 생성")
        void createProject_Success() throws Exception {
            ProjectCreateRequestDto request = new ProjectCreateRequestDto("New Project", List.of());
            ProjectResponseDto response = new ProjectResponseDto(10L, "New Project", 1, 0);
            given(projectService.createProject(any(User.class), any())).willReturn(Mono.just(response));

            MvcResult result = mockMvc.perform(post("/api/projects").with(csrf()).with(authentication(auth))
                            .contentType(MediaType.APPLICATION_JSON).content(objectMapper.writeValueAsString(request)))
                    .andExpect(request().asyncStarted()).andReturn();

            mockMvc.perform(asyncDispatch(result))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.projectName").value("New Project"));
        }

        @Test
        @DisplayName("내 프로젝트 목록 조회")
        void getMyProjects_Success() throws Exception {
            ProjectResponseDto dto = new ProjectResponseDto(10L, "My Project", 1, 0);
            given(projectService.getMyProjects(any(User.class))).willReturn(Flux.just(dto));

            MvcResult result = mockMvc.perform(get("/api/projects").with(authentication(auth)))
                    .andExpect(request().asyncStarted()).andReturn();

            mockMvc.perform(asyncDispatch(result))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$[0].projectName").value("My Project"));
        }

        @Test
        @DisplayName("프로젝트 이름 수정(OWNER만 가능)")
        void updateProjectName_Success() throws Exception {
            ProjectNameUpdateRequestDto request = new ProjectNameUpdateRequestDto("New Name");
            given(projectService.updateProjectName(anyLong(), eq(1L), anyString())).willReturn(Mono.empty());

            MvcResult result = mockMvc.perform(patch("/api/projects/10/name").with(csrf()).with(authentication(auth))
                            .contentType(MediaType.APPLICATION_JSON).content(objectMapper.writeValueAsString(request)))
                    .andExpect(request().asyncStarted()).andReturn();

            mockMvc.perform(asyncDispatch(result)).andExpect(status().isOk());
        }

        @Test
        @DisplayName("프로젝트 삭제(OWNER만 가능)")
        void deleteProject_Success() throws Exception {
            given(projectService.deleteProject(eq(10L), eq(1L))).willReturn(Mono.empty());

            MvcResult result = mockMvc.perform(delete("/api/projects/10").with(csrf()).with(authentication(auth)))
                    .andExpect(request().asyncStarted()).andReturn();

            mockMvc.perform(asyncDispatch(result)).andExpect(status().isNoContent());
        }
    }

    @Nested
    @DisplayName("멤버 초대 및 수락")
    class MemberAndInvite {
        @Test
        @DisplayName("멤버 초대 메일 발송")
        void inviteMembers_Success() throws Exception {
            ProjectInviteRequestDto request = new ProjectInviteRequestDto(List.of("friend@test.com"));
            given(projectService.inviteMembers(anyLong(), anyList())).willReturn(Mono.empty());

            MvcResult result = mockMvc.perform(post("/api/projects/10/invite").with(csrf()).with(authentication(auth))
                            .contentType(MediaType.APPLICATION_JSON).content(objectMapper.writeValueAsString(request)))
                    .andExpect(request().asyncStarted()).andReturn();

            mockMvc.perform(asyncDispatch(result)).andExpect(status().isOk());
        }

        @Test
        @DisplayName("초대 수락 - 성공 HTML 반환")
        void acceptInvitation_Success() throws Exception {
            given(projectService.acceptInvitationByToken(anyString())).willReturn(Mono.empty());
            given(templateEngine.process(eq("project/accept-success"), any())).willReturn("<html>Success</html>");

            MvcResult result = mockMvc.perform(get("/api/projects/accept")
                            .param("token", "valid-token")
                            .with(authentication(auth)))
                    .andExpect(request().asyncStarted()).andReturn();

            mockMvc.perform(asyncDispatch(result))
                    .andExpect(status().isOk())
                    .andExpect(content().contentType("text/html; charset=UTF-8"))
                    .andExpect(content().string("<html>Success</html>"));
        }

        @Test
        @DisplayName("초대 수락 - 실패 HTML 반환")
        void acceptInvitation_Fail() throws Exception {
            given(projectService.acceptInvitationByToken(anyString())).willReturn(Mono.error(new RuntimeException()));
            given(templateEngine.process(eq("project/accept-failure"), any())).willReturn("<html>Fail</html>");

            MvcResult result = mockMvc.perform(get("/api/projects/accept")
                            .param("token", "expired")
                            .with(authentication(auth)))
                    .andExpect(request().asyncStarted()).andReturn();

            mockMvc.perform(asyncDispatch(result))
                    .andExpect(status().isBadRequest())
                    .andExpect(content().string("<html>Fail</html>"));
        }
    }

    @Nested
    @DisplayName("프로젝트 멤버 및 레포지토리 관리")
    class MemberRepoManagement {
        @Test
        @DisplayName("프로젝트 멤버 목록 조회")
        void getProjectMembers_Success() throws Exception {
            ProjectMemberResponseDto member = new ProjectMemberResponseDto(1L, "testUser", "test@test.com", "http://other-image.com", ProjectRole.OWNER);
            given(projectService.getProjectMembers(10L)).willReturn(Mono.just(List.of(member)));

            MvcResult result = mockMvc.perform(get("/api/projects/10/members").with(authentication(auth)))
                    .andExpect(request().asyncStarted()).andReturn();

            mockMvc.perform(asyncDispatch(result))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$[0].username").value("testUser"))
                    .andExpect(jsonPath("$[0].profileImageUrl").value("http://other-image.com"))
                    .andExpect(jsonPath("$[0].role").value("OWNER"));
        }

        @Test
        @DisplayName("멤버 삭제(스스로 나가기)")
        void removeMember_Success() throws Exception {
            given(projectService.removeMember(10L, 1L)).willReturn(Mono.empty());

            MvcResult result = mockMvc.perform(delete("/api/projects/10/members/1").with(csrf()).with(authentication(auth)))
                    .andExpect(request().asyncStarted()).andReturn();

            mockMvc.perform(asyncDispatch(result)).andExpect(status().isNoContent());
        }

        @Test
        @DisplayName("프로젝트 레포지토리 목록 변경")
        void updateProjectRepos_Success() throws Exception {
            ProjectRepoUpdateRequestDto request = new ProjectRepoUpdateRequestDto(List.of(100L));
            given(projectService.updateProjectRepos(eq(10L), eq(1L), anyList())).willReturn(Mono.empty());

            MvcResult result = mockMvc.perform(put("/api/projects/10/repos").with(csrf()).with(authentication(auth))
                            .contentType(MediaType.APPLICATION_JSON).content(objectMapper.writeValueAsString(request)))
                    .andExpect(request().asyncStarted()).andReturn();

            mockMvc.perform(asyncDispatch(result)).andExpect(status().isOk());
        }

        @Test
        @DisplayName("프로젝트 레포지토리 목록 조회")
        void getProjectRepos_Success() throws Exception {
            GithubRepoResponseDto repo = new GithubRepoResponseDto(100L, "repo", "MAKC", "desc", "Java", 0, 0, 0, true, OffsetDateTime.now());
            given(projectService.getProjectRepoList(10L)).willReturn(Mono.just(List.of(repo)));

            MvcResult result = mockMvc.perform(get("/api/projects/10/repos").with(authentication(auth)))
                    .andExpect(request().asyncStarted()).andReturn();

            mockMvc.perform(asyncDispatch(result))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$[0].repoName").value("repo"))
                    .andExpect(jsonPath("$[0].isPublic").value(true))
                    .andExpect(jsonPath("$[0].updatedAt").exists());
        }
    }

    @Test
    @DisplayName("사용자 검색")
    void searchUsers_Success() throws Exception {
        UserSearchResponseDto user = new UserSearchResponseDto(2L, "other", "other@test.com", "http://other-image.com");
        given(projectService.searchUsers(anyString(), any(User.class))).willReturn(Flux.just(user));

        MvcResult result = mockMvc.perform(get("/api/projects/users/search").param("keyword", "other").with(authentication(auth)))
                .andExpect(request().asyncStarted()).andReturn();

        mockMvc.perform(asyncDispatch(result))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].username").value("other"))
                .andExpect(jsonPath("$[0].profileImageUrl").value("http://other-image.com"));
    }
}