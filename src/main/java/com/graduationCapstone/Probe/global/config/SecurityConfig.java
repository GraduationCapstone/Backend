package com.graduationCapstone.Probe.global.config;

import com.graduationCapstone.Probe.global.security.jwt.handler.JwtAuthenticationEntryPoint;
import com.graduationCapstone.Probe.global.security.oauth.handler.OAuth2LoginSuccessHandler;
import com.graduationCapstone.Probe.global.security.jwt.filter.JwtFilter;
import com.graduationCapstone.Probe.global.security.oauth.service.CustomOAuth2UserService;
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
                        // 모든 경로에 대해 인증 없이 허용
                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                        // GET, POST 등 일반 요청에 대해 인증 없이 접근 허용 경로
                        .requestMatchers(
                                "/",
                                "/oauth2/**",
                                "/login/**",
                                "/api/auth/reissue",
                                "/v3/api-docs/**",
                                "/swagger-ui/**"
                        ).permitAll()
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