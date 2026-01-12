package com.graduationCapstone.Probe.global.security.oauth.service;

import com.graduationCapstone.Probe.domain.user.service.UserService;
import com.graduationCapstone.Probe.global.security.oauth.dto.OAuth2ResponseDto;
import com.graduationCapstone.Probe.global.security.oauth.util.OAuth2Util;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserService;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.OAuth2AccessToken;
import org.springframework.security.oauth2.core.user.DefaultOAuth2User;
import org.springframework.security.oauth2.core.user.OAuth2User;

import java.time.Instant;
import java.util.Collections;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class CustomOAuth2UserServiceTest {

    @InjectMocks
    private CustomOAuth2UserService customOAuth2UserService;

    @Mock
    private UserService userService;

    @Mock
    private OAuth2Util oAuth2Util;

    @Mock
    private OAuth2UserService<OAuth2UserRequest, OAuth2User> mockDelegate;

    @Test
    @DisplayName("OAuth2 로그인 성공: 외부 API에서 유저 정보를 가져와 DB에 저장하고 Principal을 반환합니다.")
    void loadUser_Success() {
        // given
        customOAuth2UserService.setDelegate(mockDelegate);

        // 가짜 OAuth2 요청 데이터 생성
        String registrationId = "github";
        String userNameAttributeName = "id";

        ClientRegistration clientRegistration = ClientRegistration.withRegistrationId(registrationId)
                .clientId("client-id")
                .clientSecret("client-secret")
                .redirectUri("redirect-uri")
                .authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
                .scope("read:user")
                .authorizationUri("https://github.com/login/oauth/authorize")
                .tokenUri("https://github.com/login/oauth/access_token")
                .userInfoUri("https://api.github.com/user")
                .userNameAttributeName(userNameAttributeName)
                .clientName("GitHub")
                .build();

        OAuth2AccessToken accessToken = new OAuth2AccessToken(
                OAuth2AccessToken.TokenType.BEARER, "access-token", Instant.now(), Instant.now().plusSeconds(3600));

        OAuth2UserRequest userRequest = new OAuth2UserRequest(clientRegistration, accessToken);

        // 외부 API(Delegate)가 반환할 가짜 유저 정보 설정
        Map<String, Object> attributes = Map.of(
                "id", 12345,
                "login", "test-user",
                "email", "test@example.com"
        );
        OAuth2User mockOAuth2User = new DefaultOAuth2User(
                Collections.emptySet(), attributes, userNameAttributeName);

        // delegate.loadUser()가 호출되면 가짜 유저를 반환
        given(mockDelegate.loadUser(any(OAuth2UserRequest.class))).willReturn(mockOAuth2User);

        // OAuth2Util 변환 결과 설정
        OAuth2ResponseDto responseDto = new OAuth2ResponseDto(
                attributes, userNameAttributeName, "12345", "test-user", "test@example.com");

        given(oAuth2Util.getOAuth2Response(eq(registrationId), eq(userNameAttributeName), eq(attributes)))
                .willReturn(responseDto);

        // when
        OAuth2User result = customOAuth2UserService.loadUser(userRequest);

        // then
        // UserService가 호출되어 DB 저장 로직이 실행되었는지 검증
        verify(userService).saveOrUpdateUser(responseDto);

        // 반환된 OAuth2User 객체가 정상적인지 검증
        assertThat(result).isNotNull();
        assertThat(result.getAttributes()).isEqualTo(attributes);
        assertThat(result.getName()).isEqualTo("12345"); // userNameAttributeName("id") 값
    }
}