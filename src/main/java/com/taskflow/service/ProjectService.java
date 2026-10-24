package com.taskflow.service;

import com.taskflow.dto.AddMemberRequest;
import com.taskflow.dto.MemberResponse;
import com.taskflow.dto.ProjectRequest;
import com.taskflow.dto.ProjectResponse;
import com.taskflow.exception.ConflictException;
import com.taskflow.exception.ForbiddenException;
import com.taskflow.exception.ResourceNotFoundException;
import com.taskflow.model.Project;
import com.taskflow.model.ProjectMember;
import com.taskflow.model.ProjectRole;
import com.taskflow.model.User;
import com.taskflow.repository.ProjectMemberRepository;
import com.taskflow.repository.ProjectRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Objects;
import java.util.UUID;

@Service
@Transactional
public class ProjectService {

    private final ProjectRepository projectRepository;
    private final ProjectMemberRepository memberRepository;
    private final UserService userService;

    public ProjectService(ProjectRepository projectRepository, ProjectMemberRepository memberRepository,
                          UserService userService) {
        this.projectRepository = projectRepository;
        this.memberRepository = memberRepository;
        this.userService = userService;
    }

    public ProjectResponse create(String email, ProjectRequest request) {
        User owner = userService.findByEmail(email);
        Project project = projectRepository.save(
                new Project(request.name().trim(), request.description(), newInviteCode(), owner));
        memberRepository.save(new ProjectMember(project, owner, ProjectRole.OWNER));
        return toResponse(project, owner);
    }

    @Transactional(readOnly = true)
    public List<ProjectResponse> listForUser(String email) {
        User user = userService.findByEmail(email);
        return memberRepository.findByUserIdOrderByJoinedAtDesc(user.getId()).stream()
                .map(membership -> toResponse(membership.getProject(), user))
                .toList();
    }

    @Transactional(readOnly = true)
    public ProjectResponse get(Long projectId, String email) {
        User user = userService.findByEmail(email);
        return toResponse(requireMembership(projectId, user), user);
    }

    @Transactional(readOnly = true)
    public List<MemberResponse> listMembers(Long projectId, String email) {
        User user = userService.findByEmail(email);
        requireMembership(projectId, user);
        return memberRepository.findByProjectIdOrderByJoinedAtAsc(projectId).stream()
                .map(MemberResponse::from)
                .toList();
    }

    /** Owner adds an existing user to the project by email. */
    public MemberResponse addMember(Long projectId, String actorEmail, AddMemberRequest request) {
        User actor = userService.findByEmail(actorEmail);
        Project project = loadProject(projectId);
        if (!isOwner(project, actor)) {
            throw new ForbiddenException("Only the project owner can add members");
        }
        User invitee = userService.findByEmail(request.email());
        if (memberRepository.existsByProjectIdAndUserId(projectId, invitee.getId())) {
            throw new ConflictException("That user is already a member of this project");
        }
        return MemberResponse.from(memberRepository.save(new ProjectMember(project, invitee, ProjectRole.MEMBER)));
    }

    /** Any user holding the invite code can join. */
    public ProjectResponse join(String email, String inviteCode) {
        User user = userService.findByEmail(email);
        Project project = projectRepository.findByInviteCode(inviteCode.trim().toUpperCase())
                .orElseThrow(() -> new ResourceNotFoundException("Invalid invite code"));
        if (memberRepository.existsByProjectIdAndUserId(project.getId(), user.getId())) {
            throw new ConflictException("You are already a member of this project");
        }
        memberRepository.save(new ProjectMember(project, user, ProjectRole.MEMBER));
        return toResponse(project, user);
    }

    // ---- helpers shared with the other services ----

    /** Returns the project if the user belongs to it; 404 if it does not exist, 403 if the user is not a member. */
    @Transactional(readOnly = true)
    public Project requireMembership(Long projectId, User user) {
        Project project = loadProject(projectId);
        if (!memberRepository.existsByProjectIdAndUserId(projectId, user.getId())) {
            throw new ForbiddenException("You are not a member of this project");
        }
        return project;
    }

    @Transactional(readOnly = true)
    public boolean isMember(Long projectId, Long userId) {
        return memberRepository.existsByProjectIdAndUserId(projectId, userId);
    }

    public boolean isOwner(Project project, User user) {
        return Objects.equals(project.getOwner().getId(), user.getId());
    }

    private Project loadProject(Long projectId) {
        return projectRepository.findById(projectId)
                .orElseThrow(() -> new ResourceNotFoundException("Project " + projectId + " not found"));
    }

    private ProjectResponse toResponse(Project project, User viewer) {
        boolean viewerIsOwner = isOwner(project, viewer);
        return new ProjectResponse(
                project.getId(),
                project.getName(),
                project.getDescription(),
                project.getOwner().getId(),
                project.getOwner().getFullName(),
                memberRepository.countByProjectId(project.getId()),
                viewerIsOwner ? project.getInviteCode() : null,
                project.getCreatedAt());
    }

    private String newInviteCode() {
        return UUID.randomUUID().toString().replace("-", "").substring(0, 10).toUpperCase();
    }
}
