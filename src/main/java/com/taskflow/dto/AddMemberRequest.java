package com.taskflow.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record AddMemberRequest(
        @NotBlank(message = "Email is required")
        @Email(message = "Email is not valid")
        String email) {
}
