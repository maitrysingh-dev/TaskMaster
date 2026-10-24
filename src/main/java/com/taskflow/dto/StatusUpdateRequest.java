package com.taskflow.dto;

import com.taskflow.model.TaskStatus;
import jakarta.validation.constraints.NotNull;

public record StatusUpdateRequest(
        @NotNull(message = "Status is required")
        TaskStatus status) {
}
