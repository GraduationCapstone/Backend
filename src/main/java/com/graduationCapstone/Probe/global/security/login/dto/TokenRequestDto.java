package com.graduationCapstone.Probe.global.security.login.dto;

public record TokenRequestDto(
        String accessToken,
        String refreshToken
        ) {
}
