package com.taskflow.dto;

/** {@code assigneeId = null} un-assigns the task. */
public record AssignRequest(Long assigneeId) {
}
