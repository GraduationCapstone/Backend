package com.graduationCapstone.Probe.global.security.login.dto;

public record RefreshTokenSaveDto (
        Long userId,
        String refreshToken
){
}
