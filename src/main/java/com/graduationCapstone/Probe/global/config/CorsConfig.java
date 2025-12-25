package com.graduationCapstone.Probe.global.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;
import java.util.List;

@Configuration
public class CorsConfig {

    @Value("${cors.allowed-origins}")
    private List<String> allowedOrigins;

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();

        // 프론트엔드 URL 목록
        configuration.setAllowedOrigins(allowedOrigins);

        configuration.setAllowedHeaders(Arrays.asList("Authorization", "Content-Type"));

        configuration.setAllowedMethods(Arrays.asList("GET", "POST", "DELETE", "PATCH", "OPTIONS"));

        configuration.setAllowCredentials(true);
        configuration.setMaxAge(3600L); // 캐시 시간 1시간

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration); // 모든 경로에 CORS 설정 적용
        return source;
    }
}