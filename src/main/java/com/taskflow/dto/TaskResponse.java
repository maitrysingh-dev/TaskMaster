package com.taskflow.dto;

import com.taskflow.model.Task;
import com.taskflow.model.TaskPriority;
import com.taskflow.model.TaskStatus;

import java.time.Instant;
import java.time.LocalDate;

public record TaskResponse(Long id, String title, String description, TaskStatus status, TaskPriority priority,
                           LocalDate dueDate, Long projectId, UserSummary creator, UserSummary assignee,
                           Instant createdAt, Instant updatedAt) {

    public static TaskResponse from(Task task) {
        return new TaskResponse(task.getId(), task.getTitle(), task.getDescription(), task.getStatus(),
                task.getPriority(), task.getDueDate(), task.getProject().getId(),
                UserSummary.from(task.getCreator()), UserSummary.from(task.getAssignee()),
                task.getCreatedAt(), task.getUpdatedAt());
    }
}
