package com.snapfix.verification.dto;

import java.time.Instant;
import java.util.UUID;

import com.snapfix.verification.entity.Verification;

import lombok.Getter;

@Getter
public class VerificationResponse {
    private UUID id;
    private UUID taskId;
    private UUID citizenId;
    private VerificationStatus status;
    private String comments;
    private Instant timestamp;

    public VerificationResponse(){
        
    }

    public VerificationResponse(UUID id, UUID taskId, UUID citizenId, VerificationStatus status, String comments, Instant timestamp) {
        this.id = id;
        this.taskId = taskId;
        this.citizenId = citizenId;
        this.status = status;
        this.comments = comments;
        this.timestamp = timestamp;
    }

    public static VerificationResponse mapToResponse(Verification verification){
        return new VerificationResponse(
                verification.getId(),
                verification.getTask().getId(),
                verification.getCitizenId(),
                verification.getStatus(),
                verification.getComments(),
                verification.getTimestamp());
    }

}
