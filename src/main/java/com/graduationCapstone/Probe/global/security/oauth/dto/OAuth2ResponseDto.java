package com.graduationCapstone.Probe.global.security.oauth.dto;

import com.graduationCapstone.Probe.domain.user.entity.User;

import java.util.Map;


public record OAuth2ResponseDto (
        Map<String, Object> attributes,
        String nameAttributeKey,
        String githubId,
        String username,
        String email
){

    /**
     * DTO 데이터를 기반으로 User 엔티티 생성 (신규 가입 시 사용)
     */
    public User toEntity() {
        return User.builder()
                .githubId(this.githubId)
                .username(this.username)
                .email(this.email)
                .build();
    }
}