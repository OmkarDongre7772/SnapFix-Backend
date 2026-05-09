package com.snapfix.user.dto;

import java.util.UUID;

import com.snapfix.user.entity.Role;

import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
public class UserResponse {
    private UUID userId;
    private String email;
    private Role role;
    private ProfileDTO profile;
}
