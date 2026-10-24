package com.taskflow.repository;

import com.taskflow.model.Task;
import com.taskflow.model.TaskStatus;
import org.springframework.data.jpa.domain.Specification;

/** Small building blocks for the dynamic task filters; combined with {@code and(...)} in the service. */
public final class TaskSpecs {

    private TaskSpecs() {
    }

    public static Specification<Task> inProject(Long projectId) {
        return (root, query, cb) -> cb.equal(root.get("project").get("id"), projectId);
    }

    public static Specification<Task> assignedTo(Long userId) {
        return (root, query, cb) -> cb.equal(root.get("assignee").get("id"), userId);
    }

    public static Specification<Task> hasStatus(TaskStatus status) {
        return (root, query, cb) -> cb.equal(root.get("status"), status);
    }

    /** Case-insensitive "contains" match on title or description. */
    public static Specification<Task> matchesText(String text) {
        String pattern = "%" + text.toLowerCase() + "%";
        return (root, query, cb) -> cb.or(
                cb.like(cb.lower(root.get("title")), pattern),
                cb.like(cb.lower(root.get("description")), pattern));
    }
}
