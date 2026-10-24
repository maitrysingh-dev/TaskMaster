package com.taskflow.controller;

import com.taskflow.dto.ChangePasswordRequest;
import com.taskflow.dto.UpdateProfileRequest;
import com.taskflow.dto.UserResponse;
import com.taskflow.service.UserService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/users/me")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping
    public UserResponse getProfile(@AuthenticationPrincipal UserDetails principal) {
        return userService.getProfile(principal.getUsername());
    }

    @PutMapping
    public UserResponse updateProfile(@AuthenticationPrincipal UserDetails principal,
                                      @Valid @RequestBody UpdateProfileRequest request) {
        return userService.updateProfile(principal.getUsername(), request);
    }

    @PutMapping("/password")
    public ResponseEntity<Void> changePassword(@AuthenticationPrincipal UserDetails principal,
                                               @Valid @RequestBody ChangePasswordRequest request) {
        userService.changePassword(principal.getUsername(), request);
        return ResponseEntity.noContent().build();
    }
}
