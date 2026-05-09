package com.snapfix.user.controller;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.snapfix.user.dto.UserResponse;
import com.snapfix.user.dto.ProfileDTO;
import com.snapfix.user.dto.ProfileUpdateDto;
import com.snapfix.user.service.UserService;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/user")
public class UserController {
    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @PreAuthorize("hasAnyRole('CITIZEN', 'WORKER', 'ADMIN')")
    @GetMapping("/me")
    public UserResponse getMethodName() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String email = auth.getName();
        return userService.getCurrentUser(email);
    }

    @PreAuthorize("hasAnyRole('CITIZEN', 'WORKER')")
    @PutMapping("/profile")
    public ProfileDTO updateProfile(@Valid @RequestBody ProfileUpdateDto profileUpdateDto) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return userService.updateProfile(auth.getName(), profileUpdateDto);
    }
}
