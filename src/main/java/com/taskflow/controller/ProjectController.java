package com.taskflow.controller;

import com.taskflow.dto.AddMemberRequest;
import com.taskflow.dto.JoinProjectRequest;
import com.taskflow.dto.MemberResponse;
import com.taskflow.dto.ProjectRequest;
import com.taskflow.dto.ProjectResponse;
import com.taskflow.service.ProjectService;
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
@RequestMapping("/api/projects")
public class ProjectController {

    private final ProjectService projectService;

    public ProjectController(ProjectService projectService) {
        this.projectService = projectService;
    }

    @PostMapping
    public ResponseEntity<ProjectResponse> create(@AuthenticationPrincipal UserDetails principal,
                                                  @Valid @RequestBody ProjectRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(projectService.create(principal.getUsername(), request));
    }

    @GetMapping
    public List<ProjectResponse> listMine(@AuthenticationPrincipal UserDetails principal) {
        return projectService.listForUser(principal.getUsername());
    }

    @GetMapping("/{projectId}")
    public ProjectResponse get(@AuthenticationPrincipal UserDetails principal, @PathVariable Long projectId) {
        return projectService.get(projectId, principal.getUsername());
    }

    @PostMapping("/join")
    public ProjectResponse join(@AuthenticationPrincipal UserDetails principal,
                                @Valid @RequestBody JoinProjectRequest request) {
        return projectService.join(principal.getUsername(), request.inviteCode());
    }

    @GetMapping("/{projectId}/members")
    public List<MemberResponse> listMembers(@AuthenticationPrincipal UserDetails principal,
                                            @PathVariable Long projectId) {
        return projectService.listMembers(projectId, principal.getUsername());
    }

    @PostMapping("/{projectId}/members")
    public ResponseEntity<MemberResponse> addMember(@AuthenticationPrincipal UserDetails principal,
                                                    @PathVariable Long projectId,
                                                    @Valid @RequestBody AddMemberRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(projectService.addMember(projectId, principal.getUsername(), request));
    }
}
