package com.graduationCapstone.Probe.global.security.login.dto;

public record TokenResponseDto (
        String accessToken,
        String refreshToken
    ){
}
