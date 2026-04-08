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
import java.net.URI;
import java.util.List;

@Component
@RequiredArgsConstructor
public class OAuth2LoginSuccessHandler extends SimpleUrlAuthenticationSuccessHandler {

    private final JwtUtil jwtUtil;
    private final UserRepository userRepository;
    private final RefreshTokenService refreshTokenService;
    private final CookieUtil cookieUtil;

    // 프론트엔드 URL로 토큰을 담아 리다이렉트 (콤마로 여러 URL 지정 가능)
    @Value("${oauth2.frontend-redirect-url}")
    private List<String> frontendRedirectUrls;

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response, Authentication authentication) throws IOException {

        DefaultOAuth2User oAuth2User = (DefaultOAuth2User) authentication.getPrincipal();
        String githubId = String.valueOf(oAuth2User.getAttributes().get("id"));

        User user = userRepository.findByGithubIdAndDeletedFalse(githubId)
                .orElseThrow(() -> new IllegalArgumentException("사용자 정보를 찾을 수 없습니다."));

        String accessToken = jwtUtil.createAccessToken(user.getId());
        String refreshToken = jwtUtil.createRefreshToken(user.getId());

        refreshTokenService.saveOrUpdate(new RefreshTokenSaveDto(user.getId(), refreshToken));
        cookieUtil.addRefreshCookie(response, refreshToken);

        cookieUtil.addAccessCookie(response, accessToken);

        String targetUrl = UriComponentsBuilder.fromUriString(resolveRedirectUrl(request))
                .queryParam("access_token", accessToken)
                .build().toUriString();

        getRedirectStrategy().sendRedirect(request, response, targetUrl);
    }

    private String resolveRedirectUrl(HttpServletRequest request) {
        String referer = request.getHeader("Referer");
        if (referer != null) {
            for (String url : frontendRedirectUrls) {
                try {
                    URI redirectUri = URI.create(url.trim());
                    String origin = redirectUri.getScheme() + "://" + redirectUri.getAuthority();
                    if (referer.startsWith(origin)) {
                        return url.trim();
                    }
                } catch (IllegalArgumentException ignored) {
                }
            }
        }
        return frontendRedirectUrls.get(0).trim();
    }
}
