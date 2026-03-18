package com.graduationCapstone.Probe.global.config;

import com.graduationCapstone.Probe.global.security.jwt.handler.JwtAuthenticationEntryPoint;
import com.graduationCapstone.Probe.global.security.oauth.handler.OAuth2LoginSuccessHandler;
import com.graduationCapstone.Probe.global.security.jwt.filter.JwtFilter;
import com.graduationCapstone.Probe.global.security.oauth.service.CustomOAuth2UserService;
import com.graduationCapstone.Probe.global.security.util.CookieUtil;
import jakarta.annotation.PostConstruct;
import jakarta.servlet.DispatcherType;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfigurationSource;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final CustomOAuth2UserService customOAuth2UserService;
    private final OAuth2LoginSuccessHandler oAuth2LoginSuccessHandler;
    private final JwtFilter jwtFilter;

    private final JwtAuthenticationEntryPoint jwtAuthenticationEntryPoint;

    private final CorsConfigurationSource corsConfigurationSource;

    @PostConstruct
    public void setup() {
        org.springframework.security.core.context.SecurityContextHolder.setStrategyName(
                org.springframework.security.core.context.SecurityContextHolder.MODE_INHERITABLETHREADLOCAL
        );
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {

        http
                .cors(cors -> cors
                        .configurationSource(corsConfigurationSource))

                .csrf(AbstractHttpConfigurer::disable)

                .exceptionHandling(exception -> exception
                        .authenticationEntryPoint(this.jwtAuthenticationEntryPoint)
                )

                .sessionManagement(session -> session
                        .sessionCreationPolicy(SessionCreationPolicy.STATELESS))

                .authorizeHttpRequests(auth -> auth
                        .dispatcherTypeMatchers(DispatcherType.ASYNC).permitAll()
                        // 모든 경로에 대해 인증 없이 허용
                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                        // GET, POST 등 일반 요청에 대해 인증 없이 접근 허용 경로
                        .requestMatchers(
                                "/",
                                "/oauth2/**",
                                "/login/**",
                                "/api/auth/reissue",
                                "/api/auth/logout",
                                "/v3/api-docs/**",
                                "/swagger-ui/**",
                                "/api/projects/accept",
                                "/api/agent/callback"   // AI 서버 콜백 — VPC 내부 호출, JWT 인증 불필요
                        ).permitAll()
                        .requestMatchers("/api/github/**").authenticated()
                        .anyRequest().authenticated()
                )

                .oauth2Login(oauth2 -> oauth2
                        .successHandler(oAuth2LoginSuccessHandler)
                        .userInfoEndpoint(userInfo -> userInfo
                                .userService(customOAuth2UserService))
                );

        http.addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}