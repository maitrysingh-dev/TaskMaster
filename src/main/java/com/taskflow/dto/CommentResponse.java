package com.taskflow.dto;

import com.taskflow.model.Comment;

import java.time.Instant;

public record CommentResponse(Long id, Long taskId, UserSummary author, String content, Instant createdAt) {

    public static CommentResponse from(Comment comment) {
        return new CommentResponse(comment.getId(), comment.getTask().getId(),
                UserSummary.from(comment.getAuthor()), comment.getContent(), comment.getCreatedAt());
    }
}
