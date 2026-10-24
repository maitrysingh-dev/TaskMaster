package com.taskflow.controller;

import com.taskflow.dto.AssignRequest;
import com.taskflow.dto.CreateTaskRequest;
import com.taskflow.dto.PageResponse;
import com.taskflow.dto.StatusUpdateRequest;
import com.taskflow.dto.TaskResponse;
import com.taskflow.dto.UpdateTaskRequest;
import com.taskflow.model.TaskStatus;
import com.taskflow.service.TaskQuery;
import com.taskflow.service.TaskService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
public class TaskController {

    private final TaskService taskService;

    public TaskController(TaskService taskService) {
        this.taskService = taskService;
    }

    @PostMapping("/projects/{projectId}/tasks")
    public ResponseEntity<TaskResponse> create(@AuthenticationPrincipal UserDetails principal,
                                               @PathVariable Long projectId,
                                               @Valid @RequestBody CreateTaskRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(taskService.create(projectId, principal.getUsername(), request));
    }

    /** Tasks of one project, with optional filters on status/assignee and a free-text search. */
    @GetMapping("/projects/{projectId}/tasks")
    public PageResponse<TaskResponse> listForProject(
            @AuthenticationPrincipal UserDetails principal,
            @PathVariable Long projectId,
            @RequestParam(required = false) TaskStatus status,
            @RequestParam(required = false) Long assigneeId,
            @RequestParam(required = false, name = "q") String search,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "desc") String direction,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        TaskQuery query = new TaskQuery(projectId, status, assigneeId, search, page, size, sortBy, direction);
        return taskService.listForProject(projectId, principal.getUsername(), query);
    }

    /** Tasks assigned to the logged-in user (across all their projects, or one if projectId is given). */
    @GetMapping("/tasks/mine")
    public PageResponse<TaskResponse> listAssignedToMe(
            @AuthenticationPrincipal UserDetails principal,
            @RequestParam(required = false) Long projectId,
            @RequestParam(required = false) TaskStatus status,
            @RequestParam(required = false, name = "q") String search,
            @RequestParam(defaultValue = "dueDate") String sortBy,
            @RequestParam(defaultValue = "asc") String direction,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        TaskQuery query = new TaskQuery(projectId, status, null, search, page, size, sortBy, direction);
        return taskService.listAssignedToMe(principal.getUsername(), query);
    }

    @GetMapping("/tasks/{taskId}")
    public TaskResponse get(@AuthenticationPrincipal UserDetails principal, @PathVariable Long taskId) {
        return taskService.get(taskId, principal.getUsername());
    }

    @PutMapping("/tasks/{taskId}")
    public TaskResponse update(@AuthenticationPrincipal UserDetails principal, @PathVariable Long taskId,
                               @Valid @RequestBody UpdateTaskRequest request) {
        return taskService.update(taskId, principal.getUsername(), request);
    }

    @PatchMapping("/tasks/{taskId}/status")
    public TaskResponse updateStatus(@AuthenticationPrincipal UserDetails principal, @PathVariable Long taskId,
                                     @Valid @RequestBody StatusUpdateRequest request) {
        return taskService.updateStatus(taskId, principal.getUsername(), request);
    }

    @PatchMapping("/tasks/{taskId}/assignee")
    public TaskResponse assign(@AuthenticationPrincipal UserDetails principal, @PathVariable Long taskId,
                               @RequestBody AssignRequest request) {
        return taskService.assign(taskId, principal.getUsername(), request);
    }

    @DeleteMapping("/tasks/{taskId}")
    public ResponseEntity<Void> delete(@AuthenticationPrincipal UserDetails principal, @PathVariable Long taskId) {
        taskService.delete(taskId, principal.getUsername());
        return ResponseEntity.noContent().build();
    }
}
