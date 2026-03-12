package com.graduationCapstone.Probe.global.security.oauth.util;

import com.graduationCapstone.Probe.global.exception.handler.CustomException;
import com.graduationCapstone.Probe.global.exception.ErrorCode;
import com.graduationCapstone.Probe.global.security.oauth.dto.OAuth2ResponseDto;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@ExtendWith(MockitoExtension.class)
class OAuth2UtilTest {

    @InjectMocks
    private OAuth2Util oAuth2Util;

    @Test
    @DisplayName("GitHub 정상 변환: 모든 정보가 있을 때 DTO로 변환에 성공합니다.")
    void getOAuth2Response_Github_Success() {
        // given
        String registrationId = "github";
        String userNameAttributeName = "id";

        Map<String, Object> attributes = new HashMap<>();
        attributes.put("id", 12345);
        attributes.put("login", "testuser");
        attributes.put("email", "test@example.com");
        attributes.put("avatar_url", "https://github.com/avatar.png");

        // when
        OAuth2ResponseDto result = oAuth2Util.getOAuth2Response(registrationId, userNameAttributeName, attributes);

        // then
        assertThat(result).isNotNull();
        assertThat(result.githubId()).isEqualTo("12345");
        assertThat(result.username()).isEqualTo("testuser");
        assertThat(result.email()).isEqualTo("test@example.com");
        assertThat(result.profileImageUrl()).isEqualTo("https://github.com/avatar.png");
    }

    @Test
    @DisplayName("이메일 누락 처리: GitHub 이메일이 없을 때 임시 이메일을 생성해야 합니다.")
    void getOAuth2Response_Github_NoEmail() {
        // given
        String registrationId = "github";
        String userNameAttributeName = "id";

        Map<String, Object> attributes = new HashMap<>();
        attributes.put("id", 12345);
        attributes.put("login", "testuser");
        attributes.put("email", null); // 이메일이 없는 상황 가정

        // when
        OAuth2ResponseDto result = oAuth2Util.getOAuth2Response(registrationId, userNameAttributeName, attributes);

        // then
        assertThat(result.email()).isNotNull();

        // 로직: extractedGithubId + "_" + System.currentTimeMillis() + "@no-email.com"
        assertThat(result.email()).startsWith("12345_");
        assertThat(result.email()).endsWith("@no-email.com");
    }

    @Test
    @DisplayName("지원하지 않는 Provider: google 등이 들어오면 예외가 발생합니다.")
    void getOAuth2Response_Unsupported() {
        // given
        String registrationId = "google";
        String userNameAttributeName = "sub";
        Map<String, Object> attributes = new HashMap<>();

        // when & then
        assertThatThrownBy(() -> oAuth2Util.getOAuth2Response(registrationId, userNameAttributeName, attributes))
                .isInstanceOf(CustomException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.UNSUPPORTED_OAUTH_PROVIDER);
    }
}