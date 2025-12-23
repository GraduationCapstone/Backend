package com.graduationCapstone.Probe.global.security.oauth.handler;

import com.graduationCapstone.Probe.global.security.jwt.util.JwtUtil;
import com.graduationCapstone.Probe.domain.user.entity.User;
import com.graduationCapstone.Probe.domain.user.repository.UserRepository;
import com.graduationCapstone.Probe.global.security.login.dto.RefreshTokenSaveDto;
import com.graduationCapstone.Probe.global.security.login.service.RefreshTokenService;
import com.graduationCapstone.Probe.global.security.util.CookieUtil;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.user.DefaultOAuth2User;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.IOException;

@Component
@RequiredArgsConstructor
public class OAuth2LoginSuccessHandler extends SimpleUrlAuthenticationSuccessHandler {

    private final JwtUtil tokenProvider;
    private final UserRepository userRepository;
    private final RefreshTokenService refreshTokenService;
    private final CookieUtil cookieUtil;

    // 프론트엔드 URL로 토큰을 담아 리다이렉트
    @Value("${oauth2.frontend-redirect-url}")
    private String FRONTEND_REDIRECT_URL;

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response, Authentication authentication) throws IOException {

        DefaultOAuth2User oAuth2User = (DefaultOAuth2User) authentication.getPrincipal();
        String githubId = String.valueOf(oAuth2User.getAttributes().get("id"));

        User user = userRepository.findByGithubIdAndDeletedFalse(githubId)
                .orElseThrow(() -> new IllegalArgumentException("사용자 정보를 찾을 수 없습니다."));

        String accessToken = tokenProvider.createAccessToken(user.getId());
        String refreshToken = tokenProvider.createRefreshToken(user.getId());

        refreshTokenService.saveOrUpdate(
                new RefreshTokenSaveDto(
                        user.getId(),
                        refreshToken
                )
        );

        cookieUtil.addRefreshCookie(response, refreshToken);

        String targetUrl = UriComponentsBuilder.fromUriString(FRONTEND_REDIRECT_URL)
                .queryParam("accessToken", accessToken)
                .build().toUriString();

        getRedirectStrategy().sendRedirect(request, response, targetUrl);
    }
}
