package com.taskflow.dto;

import com.taskflow.model.TaskPriority;
import jakarta.validation.constraints.FutureOrPresent;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

public record CreateTaskRequest(
        @NotBlank(message = "Title is required")
        @Size(max = 150, message = "Title must be at most 150 characters")
        String title,

        @Size(max = 2000, message = "Description must be at most 2000 characters")
        String description,

        @FutureOrPresent(message = "Due date cannot be in the past")
        LocalDate dueDate,

        TaskPriority priority,

        Long assigneeId) {
}
