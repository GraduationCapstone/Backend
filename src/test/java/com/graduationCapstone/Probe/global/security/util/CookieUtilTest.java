package com.graduationCapstone.Probe.global.security.util;

import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
class CookieUtilTest {

    @Autowired
    private CookieUtil cookieUtil;

    @Test
    @DisplayName("Refresh Token 쿠키가 올바른 속성으로 생성되어야 합니다.")
    void createRefreshTokenCookie() {
        // given
        String refreshToken = "dummy-refresh-token";
        MockHttpServletResponse response = new MockHttpServletResponse();

        // when
        cookieUtil.addRefreshCookie(response, refreshToken);

        // then
        Cookie cookie = response.getCookie("refresh_token");
        assertThat(cookie).isNotNull();
        assertThat(cookie.getValue()).isEqualTo(refreshToken);
        assertThat(cookie.isHttpOnly()).isTrue();
        assertThat(cookie.getPath()).isEqualTo("/");
        assertThat(cookie.getMaxAge()).isGreaterThan(0);
    }

    @Test
    @DisplayName("만료된 쿠키(로그아웃용)가 정상적으로 생성되어야 합니다.")
    void removeRefreshTokenCookie() {
        // given
        MockHttpServletResponse response = new MockHttpServletResponse();

        // when
        cookieUtil.deleteRefreshCookie(response);

        // then
        Cookie cookie = response.getCookie("refresh_token");
        assertThat(cookie).isNotNull();

        // MaxAge가 0인지 검증
        assertThat(cookie.getMaxAge()).isEqualTo(0);
        assertThat(cookie.getValue()).isNull();
    }

    @Test
    @DisplayName("Request에서 쿠키 값을 올바르게 추출해야 합니다.")
    void getCookieFromRequest() {
        // given
        MockHttpServletRequest request = new MockHttpServletRequest();
        Cookie cookie = new Cookie("refresh_token", "target-token-value");
        request.setCookies(cookie);

        // when
        String extractedToken = cookieUtil.getRefreshTokenFromCookie(request);

        // then
        assertThat(extractedToken).isEqualTo("target-token-value");
    }
}