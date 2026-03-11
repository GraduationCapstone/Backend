package com.graduationCapstone.Probe.global.security.util;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Component;

import java.util.Arrays;

@Component
public class CookieUtil {

    @Value("${jwt.refresh-token-expiration-days}")
    private long refreshTokenExpirationDays;

    @Value("${jwt.access-token-expiration-minutes}")
    private long accessTokenExpirationMinutes;

    public static final String REFRESH_TOKEN_COOKIE_NAME = "refresh_token";
    public static final String ACCESS_TOKEN_COOKIE_NAME = "access_token";


    // Refresh Token 쿠키 생성
    public void addRefreshCookie(HttpServletResponse response, String refreshToken) {
        ResponseCookie cookie = ResponseCookie.from(REFRESH_TOKEN_COOKIE_NAME, refreshToken)
                .path("/")
                .httpOnly(true)
                .secure(true)
                .sameSite("None")
                .maxAge(refreshTokenExpirationDays * 24 * 60 * 60)
                .build();

        response.addHeader("Set-Cookie", cookie.toString());
    }

    // Refresh Token 쿠키 삭제 (로그아웃 시)
    public void deleteRefreshCookie(HttpServletResponse response) {
        ResponseCookie cookie = ResponseCookie.from(REFRESH_TOKEN_COOKIE_NAME, null)
                .path("/")
                .maxAge(0)
                .secure(true)
                .sameSite("None")
                .httpOnly(true)
                .build();

        response.addHeader("Set-Cookie", cookie.toString());
    }

    // Request에서 Refresh Token 가져오기
    public String getRefreshTokenFromCookie(HttpServletRequest request) {
        return getCookieValue(request, REFRESH_TOKEN_COOKIE_NAME);
    }

    public String getCookieValue(HttpServletRequest request, String name) {
        if (request.getCookies() == null) return null;
        return Arrays.stream(request.getCookies())
                .filter(cookie -> name.equals(cookie.getName()))
                .map(Cookie::getValue)
                .findFirst()
                .orElse(null);
    }

    // Access Token 쿠키 생성
    public void addAccessCookie(HttpServletResponse response, String accessToken) {
        ResponseCookie cookie = ResponseCookie.from(ACCESS_TOKEN_COOKIE_NAME, accessToken)
                .path("/")
                .httpOnly(true)
                .secure(true)
                .sameSite("None")
                .maxAge(accessTokenExpirationMinutes * 60)
                .build();

        response.addHeader("Set-Cookie", cookie.toString());
    }
}
