package com.graduationCapstone.Probe.global.security.util;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class CookieUtil {

    @Value("${jwt.refresh-token-expiration-days}")
    private long refreshTokenExpirationDays;

    public static final String REFRESH_TOKEN_COOKIE_NAME = "refresh_token";


    // Refresh Token 쿠키 생성
    public void addRefreshCookie(HttpServletResponse response, String refreshToken) {
        Cookie cookie = new Cookie(REFRESH_TOKEN_COOKIE_NAME, refreshToken);

        // HttpOnly: JavaScript 접근 불가
        cookie.setHttpOnly(true);
        // Secure: HTTPS에서만 전송
        cookie.setSecure(false);
        // 모든 경로에서 접근 가능
        cookie.setPath("/");
        // MaxAge: 초 단위로 설정
        cookie.setMaxAge((int) (refreshTokenExpirationDays * 24 * 60 * 60));

        response.addCookie(cookie);
    }

    // Refresh Token 쿠키 삭제 (로그아웃 시)
    public void deleteRefreshCookie(HttpServletResponse response) {
        Cookie cookie = new Cookie(REFRESH_TOKEN_COOKIE_NAME, null);
        cookie.setHttpOnly(true);
        cookie.setSecure(false);
        cookie.setPath("/");
        cookie.setMaxAge(0);

        response.addCookie(cookie);
    }

    // Request에서 Refresh Token 가져오기
    public String getRefreshTokenFromCookie(HttpServletRequest request) {
        Cookie[] cookies = request.getCookies();
        if (cookies != null) {
            for (Cookie cookie : cookies) {
                if (REFRESH_TOKEN_COOKIE_NAME.equals(cookie.getName())) {
                    return cookie.getValue();
                }
            }
        }
        return null;
    }
}
