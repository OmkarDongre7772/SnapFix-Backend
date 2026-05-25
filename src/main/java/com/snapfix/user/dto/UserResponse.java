package com.snapfix.user.dto;

import com.snapfix.user.entity.User;
import com.snapfix.user.entity.WorkerProfile;

import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
public class UserResponse {
    private String userId;
    private String email;
    private String role;
    private ProfileDTO profile;
    public static UserResponse mapToResponse(User worker) {
        UserResponse response = new UserResponse();
        response.setEmail(worker.getEmail());
        response.setRole(worker.getRole().toString());
        response.setUserId(worker.getId().toString());
        return response;
    }

    public static UserResponse mapToResponseWithWorkerProfile(User worker, WorkerProfile profile) {
        UserResponse response = new UserResponse();
        response.setEmail(worker.getEmail());
        response.setRole(worker.getRole().toString());
        response.setUserId(worker.getId().toString());
        response.setProfile(WorkerProfileDTO.mapToResponse(profile));
        return response;
    }
}
