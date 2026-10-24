package com.taskflow.controller;

import com.taskflow.dto.CommentRequest;
import com.taskflow.dto.CommentResponse;
import com.taskflow.service.CommentService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/tasks/{taskId}/comments")
public class CommentController {

    private final CommentService commentService;

    public CommentController(CommentService commentService) {
        this.commentService = commentService;
    }

    @PostMapping
    public ResponseEntity<CommentResponse> add(@AuthenticationPrincipal UserDetails principal,
                                               @PathVariable Long taskId,
                                               @Valid @RequestBody CommentRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(commentService.add(taskId, principal.getUsername(), request));
    }

    @GetMapping
    public List<CommentResponse> list(@AuthenticationPrincipal UserDetails principal, @PathVariable Long taskId) {
        return commentService.list(taskId, principal.getUsername());
    }
}
