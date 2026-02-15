package com.graduationCapstone.Probe.domain.project.dto;

import com.graduationCapstone.Probe.domain.project.entity.ProjectMember;

public record ProjectMemberResponseDto(
        Long userId,
        String username,
        String email
) {
    public static ProjectMemberResponseDto from(ProjectMember member) {
        return new ProjectMemberResponseDto(
                member.getUser().getId(),
                member.getUser().getUsername(),
                member.getUser().getEmail()
        );
    }
}
