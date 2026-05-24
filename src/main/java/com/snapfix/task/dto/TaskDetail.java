package com.snapfix.task.dto;

import com.snapfix.proof.dto.ProofResponse;
import com.snapfix.verification.dto.VerificationResponse;

import lombok.Getter;

@Getter
public class TaskDetail {
    private TaskResponse task;
    private ProofResponse proof;
    private VerificationResponse verification;

    public static TaskDetail mapToResponse(ProofResponse proof, VerificationResponse verification, TaskResponse task){
        TaskDetail detail = new TaskDetail();
        detail.proof = proof;
        detail.verification = verification;
        detail.task = task;
        return detail;
    }
}
