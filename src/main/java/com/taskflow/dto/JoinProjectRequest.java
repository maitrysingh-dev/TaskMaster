package com.taskflow.dto;

import jakarta.validation.constraints.NotBlank;

public record JoinProjectRequest(
        @NotBlank(message = "Invite code is required")
        String inviteCode) {
}
