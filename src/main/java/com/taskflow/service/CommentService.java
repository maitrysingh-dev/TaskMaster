package com.taskflow.service;

import com.taskflow.dto.CommentRequest;
import com.taskflow.dto.CommentResponse;
import com.taskflow.model.Comment;
import com.taskflow.model.Task;
import com.taskflow.model.User;
import com.taskflow.repository.CommentRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional
public class CommentService {

    private final CommentRepository commentRepository;
    private final TaskService taskService;
    private final UserService userService;

    public CommentService(CommentRepository commentRepository, TaskService taskService, UserService userService) {
        this.commentRepository = commentRepository;
        this.taskService = taskService;
        this.userService = userService;
    }

    public CommentResponse add(Long taskId, String email, CommentRequest request) {
        User user = userService.findByEmail(email);
        Task task = taskService.loadAccessibleTask(taskId, user);
        Comment comment = commentRepository.save(new Comment(task, user, request.content().trim()));
        return CommentResponse.from(comment);
    }

    @Transactional(readOnly = true)
    public List<CommentResponse> list(Long taskId, String email) {
        User user = userService.findByEmail(email);
        taskService.loadAccessibleTask(taskId, user);
        return commentRepository.findByTaskIdOrderByCreatedAtAsc(taskId).stream()
                .map(CommentResponse::from)
                .toList();
    }
}
