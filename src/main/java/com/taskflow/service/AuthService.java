package com.taskflow.service;

import com.taskflow.dto.AuthResponse;
import com.taskflow.dto.LoginRequest;
import com.taskflow.dto.RegisterRequest;
import com.taskflow.dto.UserResponse;
import com.taskflow.exception.ConflictException;
import com.taskflow.model.User;
import com.taskflow.repository.UserRepository;
import com.taskflow.security.JwtService;
import com.taskflow.security.TokenBlacklistService;
import io.jsonwebtoken.Claims;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;
    private final TokenBlacklistService blacklist;

    public AuthService(UserRepository userRepository, PasswordEncoder passwordEncoder,
                       AuthenticationManager authenticationManager, JwtService jwtService,
                       TokenBlacklistService blacklist) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.authenticationManager = authenticationManager;
        this.jwtService = jwtService;
        this.blacklist = blacklist;
    }

    @Transactional
    public AuthResponse register(RegisterRequest request) {
        String email = request.email().trim().toLowerCase();
        if (userRepository.existsByEmail(email)) {
            throw new ConflictException("An account with this email already exists");
        }
        User user = userRepository.save(
                new User(request.fullName().trim(), email, passwordEncoder.encode(request.password())));
        return buildResponse(user);
    }

    @Transactional(readOnly = true)
    public AuthResponse login(LoginRequest request) {
        String email = request.email().trim().toLowerCase();
        // Throws BadCredentialsException (-> 401) when the email/password pair is wrong.
        authenticationManager.authenticate(new UsernamePasswordAuthenticationToken(email, request.password()));
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new BadCredentialsException("Invalid email or password"));
        return buildResponse(user);
    }

    public void logout(String token) {
        Claims claims = jwtService.parse(token);
        blacklist.revoke(claims.getId(), claims.getExpiration().toInstant());
    }

    private AuthResponse buildResponse(User user) {
        String token = jwtService.generateToken(user.getEmail());
        return new AuthResponse(token, "Bearer", jwtService.getExpirationMs(), UserResponse.from(user));
    }
}
