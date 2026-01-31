package com.graduationCapstone.Probe.global.security.oauth.service;

import com.graduationCapstone.Probe.domain.user.entity.User;
import com.graduationCapstone.Probe.domain.user.service.UserService;
import com.graduationCapstone.Probe.global.security.oauth.dto.OAuth2ResponseDto;
import com.graduationCapstone.Probe.global.security.oauth.util.OAuth2Util;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserService;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.user.DefaultOAuth2User;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class CustomOAuth2UserService extends DefaultOAuth2UserService {

    private final UserService userService;
    private final OAuth2Util oAuth2Util;

    /**
     * 기본 OAuth2UserService 구현체입니다. 실제 사용자 정보를 로드하는 역할을 위임받습니다.
     * 테스트 환경 등에서 커스텀 구현체로 교체 가능하도록 Setter를 열어두었습니다.
     */
    @Setter
    private OAuth2UserService<OAuth2UserRequest, OAuth2User> delegate = new DefaultOAuth2UserService();

    /**
     * GitHub 인증 정보를 바탕으로 사용자 객체를 생성하거나 갱신합니다.
     * GitHub가 발급한 AccessToken을 추출하여 User 엔티티에 저장하며,
     * Spring Security의 Authentication 객체에 저장될 OAuth2User를 반환합니다.
     *
     * @param userRequest 클라이언트의 등록 정보 및 AccessToken을 포함한 요청 정보
     * @return 인증된 사용자 정보와 권한을 담은 OAuth2User 객체
     * @throws OAuth2AuthenticationException 인증 프로세스 중 오류 발생 시
     */
    @Override
    @Transactional
    public OAuth2User loadUser(OAuth2UserRequest userRequest) throws OAuth2AuthenticationException {

        OAuth2User oAuth2User = delegate.loadUser(userRequest);

        // Github가 발급한 실제 AccessToken 추출(Github 서비스 전용 토큰)
        String githubAccessToken = userRequest.getAccessToken().getTokenValue();
        Map<String, Object> attributes = oAuth2User.getAttributes();

        String registrationId = userRequest.getClientRegistration().getRegistrationId();
        String userNameAttributeName = userRequest.getClientRegistration().getProviderDetails()
                .getUserInfoEndpoint().getUserNameAttributeName();

        OAuth2ResponseDto oAuth2ResponseDto = oAuth2Util.getOAuth2Response(
                registrationId,
                userNameAttributeName,
                attributes
        );

        // 리턴받은 user 객체에 GithubAccessToken 업데이트
        User user = userService.saveOrUpdateUser(oAuth2ResponseDto);
        userService.updateGithubToken(user.getId(), githubAccessToken);

        Map<String, Object> modifiedAttributes = new HashMap<>(attributes);
        modifiedAttributes.put("githubAccessToken", githubAccessToken);

        // 권한 구분 없음
        return new DefaultOAuth2User(
                Collections.singleton(new SimpleGrantedAuthority("ROLE_USER")),
                modifiedAttributes,
                oAuth2ResponseDto.nameAttributeKey());
    }
}