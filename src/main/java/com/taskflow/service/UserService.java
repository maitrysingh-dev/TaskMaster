package com.taskflow.service;

import com.taskflow.dto.ChangePasswordRequest;
import com.taskflow.dto.UpdateProfileRequest;
import com.taskflow.dto.UserResponse;
import com.taskflow.exception.BadRequestException;
import com.taskflow.exception.ResourceNotFoundException;
import com.taskflow.model.User;
import com.taskflow.repository.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public UserService(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional(readOnly = true)
    public User findByEmail(String email) {
        return userRepository.findByEmail(email.trim().toLowerCase())
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
    }

    @Transactional(readOnly = true)
    public User findById(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User " + id + " not found"));
    }

    @Transactional(readOnly = true)
    public UserResponse getProfile(String email) {
        return UserResponse.from(findByEmail(email));
    }

    public UserResponse updateProfile(String email, UpdateProfileRequest request) {
        User user = findByEmail(email);
        user.setFullName(request.fullName().trim());
        return UserResponse.from(user);
    }

    public void changePassword(String email, ChangePasswordRequest request) {
        User user = findByEmail(email);
        if (!passwordEncoder.matches(request.currentPassword(), user.getPasswordHash())) {
            throw new BadRequestException("Current password is incorrect");
        }
        user.setPasswordHash(passwordEncoder.encode(request.newPassword()));
    }
}
