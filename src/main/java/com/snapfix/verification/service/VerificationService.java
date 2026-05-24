package com.snapfix.verification.service;

import com.snapfix.auth.security.CustomUserDetails;
import com.snapfix.task.entity.Task;
import com.snapfix.task.entity.TaskStatus;
import com.snapfix.task.service.TaskService;
import com.snapfix.verification.repository.VerificationRepository;

import java.time.Instant;
import java.util.UUID;

import jakarta.transaction.Transactional;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import com.snapfix.verification.dto.VerificationResponse;
import com.snapfix.verification.dto.VerificationStatus;
import com.snapfix.verification.entity.Verification;

@Service
public class VerificationService {

    private static final int MAX_RETRY_COUNT = 3;

    private final TaskService taskService;
    private final VerificationRepository verificationRepository;

    VerificationService(VerificationRepository verificationRepository, TaskService taskService) {
        this.verificationRepository = verificationRepository;
        this.taskService = taskService;
    }

/*
        SERVICE FUNCTIONS---------------
*/

    @Transactional
    public VerificationResponse verifyTask(UUID taskId, VerificationStatus status, String comments) {
        if (status == null) {
            throw new IllegalArgumentException("Verification status is required");
        }
        if (verificationRepository.existsByTask_Id(taskId)) {
            throw new IllegalStateException("Task has already been verified");
        }

        Task task = taskService.getTaskById(taskId);
        if (!task.getReport().getCitizenId().equals(getCurrentUserId())) {
            throw new AccessDeniedException("Only the report owner can verify the task");
        }

        if (task.getStatus() != TaskStatus.PROOF_SUBMITTED) {
            throw new IllegalStateException("Task must be PROOF_SUBMITTED before verification");
        }

        Verification verification = new Verification();
        verification.setTask(task);
        verification.setStatus(status);
        verification.setCitizenId(task.getReport().getCitizenId());
        verification.setTimestamp(Instant.now());

        if (VerificationStatus.VERIFIED.equals(status)) {
            verification.setComments(resolveComments(comments, "Verified by citizen."));
            task.setStatus(TaskStatus.VERIFIED_BY_CITIZEN);
        } else if (VerificationStatus.REJECTED.equals(status)) {
            if (task.getRetryCount() >= MAX_RETRY_COUNT) {
                throw new IllegalStateException("Maximum retry limit reached");
            }
            verification.setComments(resolveComments(comments, "Rejected by citizen."));
            task.setStatus(TaskStatus.REJECTED);
        }

        taskService.saveTask(task);
        Verification savedVerification = verificationRepository.save(verification);
        return VerificationResponse.mapToResponse(savedVerification);
    }

    @Transactional
    public VerificationResponse autoVerifyTask(UUID taskId) {
        if (verificationRepository.existsByTask_Id(taskId)) {
            throw new IllegalStateException("Task has already been verified");
        }

        Task task = taskService.getTaskById(taskId);
        if (task.getStatus() != TaskStatus.PROOF_SUBMITTED) {
            throw new IllegalStateException("Task must be PROOF_SUBMITTED before verification");
        }

        Verification verification = new Verification();
        verification.setTask(task);
        verification.setStatus(VerificationStatus.VERIFIED);
        verification.setCitizenId(task.getReport().getCitizenId());
        verification.setComments("Auto-verified due to citizen inactivity");
        verification.setTimestamp(Instant.now());
        task.setStatus(TaskStatus.VERIFIED_BY_CITIZEN);

        taskService.saveTask(task);
        Verification savedVerification = verificationRepository.save(verification);
        return VerificationResponse.mapToResponse(savedVerification);
    }

    public Verification getVerificationByTaskId(UUID id) {
        return verificationRepository.findByTask_Id(id)
                .orElseThrow(() -> new IllegalStateException("Task not found for respective Verification id"));
    }

/*
        UTILITY FUNCTIONS---------------
*/

    private UUID getCurrentUserId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();

        if (!(auth.getPrincipal() instanceof CustomUserDetails user)) {
            throw new IllegalStateException("Invalid authentication context");
        }

        return user.getId();
    }

    private String resolveComments(String comments, String defaultComments) {
        if (comments == null || comments.isBlank()) {
            return defaultComments;
        }
        if (comments.length() > 1000) {
            throw new IllegalArgumentException("Verification comments must be at most 1000 characters");
        }
        return comments;
    }

}
