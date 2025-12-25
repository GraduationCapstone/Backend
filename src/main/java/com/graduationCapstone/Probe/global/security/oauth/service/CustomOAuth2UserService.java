package com.graduationCapstone.Probe.global.security.oauth.service;

import com.graduationCapstone.Probe.domain.user.service.UserService;
import com.graduationCapstone.Probe.global.security.oauth.dto.OAuth2ResponseDto;
import com.graduationCapstone.Probe.global.security.oauth.util.OAuth2Util;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserService;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.user.DefaultOAuth2User;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class CustomOAuth2UserService extends DefaultOAuth2UserService {

    private final UserService userService;
    private final OAuth2Util oAuth2Util;

    @Override
    @Transactional
    public OAuth2User loadUser(OAuth2UserRequest userRequest) throws OAuth2AuthenticationException {

        OAuth2UserService<OAuth2UserRequest, OAuth2User> delegate = new DefaultOAuth2UserService();
        OAuth2User oAuth2User = delegate.loadUser(userRequest);

        Map<String, Object> attributes = oAuth2User.getAttributes();

        String registrationId = userRequest.getClientRegistration().getRegistrationId();
        String userNameAttributeName = userRequest.getClientRegistration().getProviderDetails()
                .getUserInfoEndpoint().getUserNameAttributeName();

        OAuth2ResponseDto oAuth2ResponseDto = oAuth2Util.getOAuth2Response(
                registrationId,
                userNameAttributeName,
                attributes
        );

        userService.saveOrUpdateUser(oAuth2ResponseDto);

        // 권한 구분 없음
        return new DefaultOAuth2User(
                Collections.singleton(new SimpleGrantedAuthority("ROLE_USER")),
               oAuth2ResponseDto.attributes(),
                oAuth2ResponseDto.nameAttributeKey());
    }
}