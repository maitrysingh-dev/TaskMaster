package com.taskflow.repository;

import com.taskflow.model.Task;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface TaskRepository extends JpaRepository<Task, Long>, JpaSpecificationExecutor<Task> {

    // Fetch the associations we always render so a page of tasks doesn't trigger N+1 selects.
    @Override
    @EntityGraph(attributePaths = {"creator", "assignee", "project"})
    Page<Task> findAll(Specification<Task> spec, Pageable pageable);
}
