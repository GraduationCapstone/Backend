package com.graduationCapstone.Probe.infrastructure.github.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.web.reactive.function.client.WebClient;

@Configuration
public class GitHubConfig {

    @Value("${github.api.url:https://api.github.com}")
    private String apiUrl;

    @Value("${github.api.pat}")
    private String pat;

    @Bean
    public WebClient githubWebClient() {
        // WebClient는 스레드 안전하므로 싱글톤 빈으로 등록하여 재사용합니다.
        return WebClient.builder()
                .baseUrl(apiUrl)
                .defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + pat)
                .defaultHeader(HttpHeaders.ACCEPT, "application/vnd.github+json")
                .defaultHeader("X-GitHub-Api-Version", "2022-11-28")
                .build();
    }
}
