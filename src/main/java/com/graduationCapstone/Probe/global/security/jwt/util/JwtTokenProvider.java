package com.graduationCapstone.Probe.global.security.jwt.util;

import io.jsonwebtoken.*;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.security.Key;
import java.util.Date;

@Slf4j
@Component
public class JwtTokenProvider {

    private final Key key;
    private final long accessTokenValidityInMilliseconds;
    private final long refreshTokenValidityInMilliseconds;

    public JwtTokenProvider(
            @Value("${jwt.secret}") String secretKey,
            @Value("${jwt.access-token-expiration-minutes}") long accessExpirationMinutes,
            @Value("${jwt.refresh-token-expiration-days}") long refreshExpirationDays) {

        // JWT 서명에 사용할 Key 설정
        byte[] keyBytes = Decoders.BASE64.decode(secretKey);
        this.key = Keys.hmacShaKeyFor(keyBytes);

        // 만료 시간 설정 (분 -> 밀리초)
        this.accessTokenValidityInMilliseconds = accessExpirationMinutes * 60 * 1000;

        // 만료 시간 설정 (일 -> 밀리초)
        this.refreshTokenValidityInMilliseconds = refreshExpirationDays * 24 * 60 * 60 * 1000;
    }

    /**
     * Access Token을 생성합니다.
     * @param userId 토큰에 담을 사용자 고유 ID
     * @return 생성된 JWT 문자열
     */
    public String createAccessToken(Long userId) {
        return createToken(userId, accessTokenValidityInMilliseconds);
    }

    /**
     * Refresh Token을 생성합니다.
     * @param userId 토큰에 담을 사용자 고유 ID
     * @return 생성된 JWT 문자열
     */
    public String createRefreshToken(Long userId) {
        return createToken(userId, refreshTokenValidityInMilliseconds);
    }

    /**
     * JWT 토큰 생성 공통 로직
     */
    private String createToken(Long userId, long validity) {
        Date now = new Date();
        Date validityDate = new Date(now.getTime() + validity);

        Claims claims = Jwts.claims().setSubject(String.valueOf(userId));
        // claims.put("role", role); // Role을 사용하지 않으므로 주석 처리함

        return Jwts.builder()
                .setClaims(claims) // 정보 저장
                .setIssuedAt(now) // 토큰 발행 시간
                .setExpiration(validityDate) // 토큰 만료 시간
                .signWith(key, SignatureAlgorithm.HS256) // 서명 키, 알고리즘 설정
                .compact();
    }

    /**
     * 토큰에서 사용자 ID를 추출합니다.
     * @param token JWT 토큰
     * @return 사용자 ID (Long)
     */
    public Long getUserId(String token) {
        return Long.valueOf(Jwts.parserBuilder()
                .setSigningKey(key)
                .build()
                .parseClaimsJws(token)
                .getBody()
                .getSubject());
    }

    /**
     * 토큰의 유효성을 검사합니다.
     * @param token JWT 토큰
     * @return 유효성 여부 (true: 유효, false: 유효하지 않음)
     */
    public boolean validateToken(String token) {
        try {
            Jwts.parserBuilder().setSigningKey(key).build().parseClaimsJws(token);
            return true;
        } catch (io.jsonwebtoken.security.SecurityException | MalformedJwtException e) {
            log.info("잘못된 JWT 서명입니다.");
        } catch (ExpiredJwtException e) {
            log.info("만료된 JWT 토큰입니다.");
        } catch (UnsupportedJwtException e) {
            log.info("지원되지 않는 JWT 토큰입니다.");
        } catch (IllegalArgumentException e) {
            log.info("잘못된 JWT 토큰입니다.");
        }
        return false;
    }
}