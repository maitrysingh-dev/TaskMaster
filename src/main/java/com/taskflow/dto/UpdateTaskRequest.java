package com.taskflow.dto;

import com.taskflow.model.TaskPriority;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

/** Used by PUT: replaces the editable fields. Status and assignee have their own endpoints. */
public record UpdateTaskRequest(
        @NotBlank(message = "Title is required")
        @Size(max = 150, message = "Title must be at most 150 characters")
        String title,

        @Size(max = 2000, message = "Description must be at most 2000 characters")
        String description,

        LocalDate dueDate,

        TaskPriority priority) {
}
