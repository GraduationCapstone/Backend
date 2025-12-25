package com.graduationCapstone.Probe.global.security.oauth.util;

import com.graduationCapstone.Probe.global.exception.ErrorCode;
import com.graduationCapstone.Probe.global.exception.handler.CustomException;
import com.graduationCapstone.Probe.global.security.oauth.dto.OAuth2ResponseDto;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
public class OAuth2Util {

    /**
     * 외부에서 들어온 데이터 OAuth2ResponseDto로 변환
     */
    public OAuth2ResponseDto getOAuth2Response(
            String registrationId,
            String userNameAttributeName,
            Map<String, Object> attributes) {

        if ("github".equals(registrationId)) {
            return ofGithub(userNameAttributeName, attributes);
        }

        throw new CustomException(ErrorCode.UNSUPPORTED_OAUTH_PROVIDER);
    }

    /**
     * GitHub 전용 사용자 정보 추출 로직
     */
    private static OAuth2ResponseDto ofGithub(String userNameAttributeName, Map<String, Object> attributes) {

        String extractedGithubId = String.valueOf(attributes.get(userNameAttributeName));
        String extractedUsername = (String) attributes.get("login");
        String extractedEmail = (String) attributes.get("email");

        // GithubID 사용하여 서비스 내에서 고유성 보장하는 임시 이메일 생성
        // 이메일 형식: [githubId]_[Timestamp]@no-email.com
        if (extractedEmail == null || extractedEmail.isEmpty()) {
            extractedEmail = extractedGithubId + "_" + System.currentTimeMillis() + "@no-email.com";
        }

        return new OAuth2ResponseDto(
                attributes,
                userNameAttributeName,
                extractedGithubId,
                extractedUsername,
                extractedEmail
        );
    }
}
