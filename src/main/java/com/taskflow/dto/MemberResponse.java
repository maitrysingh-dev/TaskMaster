package com.taskflow.dto;

import com.taskflow.model.ProjectMember;
import com.taskflow.model.ProjectRole;

import java.time.Instant;

public record MemberResponse(Long userId, String fullName, String email, ProjectRole role, Instant joinedAt) {

    public static MemberResponse from(ProjectMember member) {
        return new MemberResponse(member.getUser().getId(), member.getUser().getFullName(),
                member.getUser().getEmail(), member.getRole(), member.getJoinedAt());
    }
}
