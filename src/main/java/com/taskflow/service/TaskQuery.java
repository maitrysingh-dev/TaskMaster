package com.taskflow.service;

import com.taskflow.model.TaskStatus;

/** Filter + paging + sorting options for task listings. */
public record TaskQuery(Long projectId, TaskStatus status, Long assigneeId, String search,
                        int page, int size, String sortBy, String direction) {
}
