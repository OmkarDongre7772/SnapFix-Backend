package com.snapfix.auth.controller;

import com.snapfix.auth.dto.AuthResponse;
import com.snapfix.auth.dto.LoginRequest;
import com.snapfix.auth.dto.LogoutRequest;
import com.snapfix.auth.dto.RegisterRequest;
import com.snapfix.auth.service.AuthService;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/auth")
public class AuthController {
    
    private static final Logger log =
            LoggerFactory.getLogger(AuthController.class);
    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/register")
    public ResponseEntity<String> register(@Valid @RequestBody RegisterRequest request) {
        log.info("Register request received for email: {}", request.getEmail());
        authService.register(request);
        return ResponseEntity.ok("User registered successfully");
    }

    @PostMapping("/login")
    public AuthResponse login(@Valid @RequestBody LoginRequest request) {
        log.info("Login request received for email: {}", request.getEmail());
        return authService.login(request.getEmail(), request.getPassword());
    }

    @PostMapping("/refresh")
    public AuthResponse refresh(@RequestBody String refreshToken) {
        return authService.refresh(refreshToken);
    }

    @PostMapping("/logout")
    public ResponseEntity<String> logout(@Valid @RequestBody LogoutRequest request, HttpServletRequest httpRequest) {
        String accessToken = null;
        String authHeader = httpRequest.getHeader("Authorization");
        if(authHeader != null && authHeader.startsWith("Bearer")){
            accessToken = authHeader.substring(7);
        }
        authService.logout(request.getRefreshToken(), accessToken);
        return ResponseEntity.ok("Logged out successfully");
    }
}
