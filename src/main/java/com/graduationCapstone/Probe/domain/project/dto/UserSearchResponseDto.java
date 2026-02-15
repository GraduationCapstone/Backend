package com.graduationCapstone.Probe.domain.project.dto;

import com.graduationCapstone.Probe.domain.user.entity.User;

public record UserSearchResponseDto(
        Long userId,
        String username,
        String email
) {
    public static UserSearchResponseDto from(User user) {
        return new UserSearchResponseDto(
                user.getId(),
                user.getUsername(),
                user.getEmail()
        );
    }
}