package com.taskflow.repository;

import com.taskflow.model.ProjectMember;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ProjectMemberRepository extends JpaRepository<ProjectMember, Long> {

    boolean existsByProjectIdAndUserId(Long projectId, Long userId);

    List<ProjectMember> findByProjectIdOrderByJoinedAtAsc(Long projectId);

    List<ProjectMember> findByUserIdOrderByJoinedAtDesc(Long userId);

    long countByProjectId(Long projectId);
}
