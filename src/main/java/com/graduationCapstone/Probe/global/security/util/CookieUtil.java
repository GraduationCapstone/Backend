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
    public static final String JSESSIONID_COOKIE_NAME = "JSESSIONID";


    public void addRefreshCookie(HttpServletResponse response, String refreshToken) {
        addCookie(response, REFRESH_TOKEN_COOKIE_NAME, refreshToken, refreshTokenExpirationDays * 24 * 60 * 60);
    }

    public void addAccessCookie(HttpServletResponse response, String accessToken) {
        addCookie(response, ACCESS_TOKEN_COOKIE_NAME, accessToken, accessTokenExpirationMinutes * 60);
    }

    /**
     * 모든 인증 관련 쿠키 일괄 삭제 (로그아웃/탈퇴 시)
     */
    public void deleteAllCookies(HttpServletResponse response) {
        deleteCookie(response, REFRESH_TOKEN_COOKIE_NAME);
        deleteCookie(response, ACCESS_TOKEN_COOKIE_NAME);
        deleteCookie(response, JSESSIONID_COOKIE_NAME);
    }

    /**
     * 개별 쿠키 삭제 메서드 (필요 시 선택적 사용)
     */
    public void deleteRefreshCookie(HttpServletResponse response) {
        deleteCookie(response, REFRESH_TOKEN_COOKIE_NAME);
    }

    public void deleteAccessCookie(HttpServletResponse response) {
        deleteCookie(response, ACCESS_TOKEN_COOKIE_NAME);
    }

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

    private void addCookie(HttpServletResponse response, String name, String value, long maxAge) {
        ResponseCookie cookie = ResponseCookie.from(name, value)
                .path("/")
                .httpOnly(true)
                .secure(true)
                .sameSite("None")
                .maxAge(maxAge)
                .build();
        response.addHeader("Set-Cookie", cookie.toString());
    }

    private void deleteCookie(HttpServletResponse response, String name) {
        ResponseCookie cookie = ResponseCookie.from(name, "")
                .path("/")
                .maxAge(0)
                .httpOnly(true)
                .secure(true)
                .sameSite("None")
                .build();
        response.addHeader("Set-Cookie", cookie.toString());
    }
}

