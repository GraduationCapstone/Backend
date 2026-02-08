package com.graduationCapstone.Probe.domain.github.controller;

import com.graduationCapstone.Probe.domain.github.dto.*;
import com.graduationCapstone.Probe.domain.github.service.GithubRepoService;
import com.graduationCapstone.Probe.domain.github.service.GithubService;
import com.graduationCapstone.Probe.domain.user.entity.User;
import com.graduationCapstone.Probe.domain.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.WebFluxTest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.reactive.server.WebTestClient;
import reactor.core.publisher.Mono;

import java.util.Optional;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.reactive.server.SecurityMockServerConfigurers.csrf;
import static org.springframework.security.test.web.reactive.server.SecurityMockServerConfigurers.mockAuthentication;

@WebFluxTest(GithubController.class)
class GithubControllerTest {

    @Autowired
    private WebTestClient webTestClient;

    @MockitoBean
    private GithubRepoService githubRepoService;

    @MockitoBean
    private GithubService githubService;

    @MockitoBean
    private UserRepository userRepository;

    private User testUser;
    private UsernamePasswordAuthenticationToken auth;

    @BeforeEach
    void setUp() {
        testUser = User.builder()
                .id(1L)
                .username("testUser")
                .githubAccessToken("token")
                .build();

        auth = new UsernamePasswordAuthenticationToken(testUser, null, java.util.Collections.emptyList());
    }

    @Test
    @DisplayName("전체 코드 추출 API 요청 성공을 검증합니다.")
    void getFullCodeApiTest() {
        when(userRepository.findById(anyLong())).thenReturn(Optional.of(testUser));

        when(githubService.getRepoFullCode(anyString(), anyString(), anyString()))
                .thenReturn(Mono.just(new GithubRepoDto("Probe", java.util.List.of())));

        webTestClient
                .mutateWith(mockAuthentication(auth))
                .mutateWith(csrf())
                .get().uri("/api/github/repos/Probe/code")
                .exchange()
                .expectStatus().isOk();
    }

    @Test
    @DisplayName("레포지토리 삭제 성공 시 204 No Content를 반환합니다.")
    void deleteRepoApiTest() {
        when(githubRepoService.deleteRepo(anyLong(), anyLong())).thenReturn(Mono.empty());

        webTestClient
                .mutateWith(mockAuthentication(auth))
                .mutateWith(csrf())
                .delete().uri("/api/github/repos/1")
                .exchange()
                .expectStatus().isNoContent();
    }
}