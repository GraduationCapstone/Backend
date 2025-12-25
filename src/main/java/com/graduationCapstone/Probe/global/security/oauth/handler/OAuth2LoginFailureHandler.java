package com.graduationCapstone.Probe.global.security.oauth.handler;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;
import org.springframework.stereotype.Component;
import org.springframework.security.core.AuthenticationException;

import java.io.IOException;

@Component
@Slf4j
public class OAuth2LoginFailureHandler implements AuthenticationFailureHandler {

    @Override
    public void onAuthenticationFailure(HttpServletRequest request, HttpServletResponse response, AuthenticationException exception) throws IOException, ServletException {
        // 인증 실패시 지정해둔 경로로 이동(추후 수정)
        response.sendRedirect("http://localhost:8080/");
    }
}
