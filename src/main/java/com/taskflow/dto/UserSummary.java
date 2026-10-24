package com.taskflow.dto;

import com.taskflow.model.User;

/** Minimal user info embedded in tasks, comments and attachments. */
public record UserSummary(Long id, String fullName) {

    public static UserSummary from(User user) {
        return user == null ? null : new UserSummary(user.getId(), user.getFullName());
    }
}
