package com.graduationCapstone.Probe.global.security.jwt.util;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;

import java.security.Key;
import java.util.Date;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
class JwtUtilTest {

    @Autowired
    private JwtUtil jwtUtil;

    @Value("${jwt.secret}")
    private String secretKey;

    @Test
    @DisplayName("Access Token이 정상적으로 생성되고, ID가 추출되어야 합니다.")
    void createAndParseToken() {
        // given
        Long userId = 1L;

        // when
        String token = jwtUtil.createAccessToken(userId);
        Long extractedId = jwtUtil.getUserId(token);

        // then
        assertThat(token).isNotNull();
        assertThat(extractedId).isEqualTo(userId);
        assertThat(jwtUtil.validateToken(token)).isTrue();
    }

    @Test
    @DisplayName("만료된 토큰은 유효성 검사에서 false를 반환해야 합니다.")
    void validateExpiredToken() {
        // given
        // 현재 시간보다 과거로 만료 시간을 설정하여 토큰 생성
        Date past = new Date(System.currentTimeMillis() - 1000 * 60); // 1분 전
        Key key = Keys.hmacShaKeyFor(Decoders.BASE64.decode(secretKey));

        String expiredToken = Jwts.builder()
                .setSubject("1")
                .setExpiration(past) // 이미 만료됨
                .signWith(key, SignatureAlgorithm.HS256)
                .compact();

        // when
        boolean isValid = jwtUtil.validateToken(expiredToken);

        // then
        assertThat(isValid).isFalse();
    }

    @Test
    @DisplayName("서명이 잘못된 토큰은 유효성 검사에서 false를 반환해야 합니다.")
    void validateInvalidSignatureToken() {
        // given
        // 정상 토큰 생성
        String token = jwtUtil.createAccessToken(1L);

        // 토큰의 끝부분(서명)을 조작
        String tamperedToken = token.substring(0, token.length() - 5) + "fake";

        // when
        boolean isValid = jwtUtil.validateToken(tamperedToken);

        // then
        assertThat(isValid).isFalse();
    }

    @Test
    @DisplayName("빈 문자열이나 null은 유효성 검사에서 false를 반환해야 합니다.")
    void validateEmptyToken() {
        assertThat(jwtUtil.validateToken("")).isFalse();
        assertThat(jwtUtil.validateToken(null)).isFalse();
    }
}
