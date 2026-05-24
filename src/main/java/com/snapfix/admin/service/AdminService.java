package com.snapfix.admin.service;

import com.snapfix.bid.entity.Bid;
import com.snapfix.bid.service.BidService;
import com.snapfix.proof.service.ProofService;
import com.snapfix.report.entity.ReportStatus;
import com.snapfix.task.dto.TaskDetail;
import com.snapfix.task.dto.TaskResponse;
import com.snapfix.task.entity.Task;
import com.snapfix.task.entity.TaskStatus;
import com.snapfix.task.service.TaskService;
import com.snapfix.user.entity.User;

import com.snapfix.user.service.UserService;
import com.snapfix.verification.dto.VerificationResponse;
import com.snapfix.verification.service.VerificationService;
import jakarta.transaction.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import com.snapfix.admin.entity.AdminActionLog;
import com.snapfix.admin.repository.AdminRepository;
import com.snapfix.auth.security.CustomUserDetails;

@Service
public class AdminService {
    private final VerificationService verificationService;
    private final ProofService proofService;
    private final TaskService taskService;
    private final BidService bidService;
    private final AdminRepository adminRepository;
    private final UserService userService;

    public AdminService(
            AdminRepository adminRepository, BidService bidService, UserService userService, TaskService taskService,
            ProofService proofService, VerificationService verificationService) {
        this.adminRepository = adminRepository;
        this.bidService = bidService;
        this.userService = userService;
        this.taskService = taskService;
        this.proofService = proofService;
        this.verificationService = verificationService;
    }

    @Transactional
    public void approveBid(UUID bidId) {
        Bid bid = bidService.approveBid(bidId);
        User admin = userService.getUserById(getCurrentUserId());
        log(admin, bid.getWorker().getId(),
                "Bid Approved By " + getCurrentUserId() + ", for Worker " + bid.getWorker().getId(), "");
    }

    @Transactional
    public void rejectBid(UUID bidId) {
        Bid bid = bidService.rejectBid(bidId);
        User admin = userService.getUserById(getCurrentUserId());
        log(admin, bid.getWorker().getId(),
                "Bid Rejected By " + getCurrentUserId() + ", for Worker " + bid.getWorker().getId(), "");
    }

    @Transactional
    public List<TaskResponse> getTasksByStatus(TaskStatus status) {
        List<Task> tasks = taskService.getTasksByStatus(status);
        return tasks.stream().map(t -> TaskResponse.mapTask(t)).toList();
    }

    public TaskDetail getTaskDetail(UUID id) {
        return TaskDetail.mapToResponse(
                proofService.getTaskProof(id),
                VerificationResponse.mapToResponse(
                        verificationService.getVerificationByTaskId(id)),
                TaskResponse.mapTask(taskService.getTaskById(id)));
    }

    @Transactional
    public TaskResponse approveTask(UUID taskId) {
        TaskResponse response = taskService.approveTask(taskId);
        log(userService.getUserById(getCurrentUserId()), taskId, "Task Approved",
                "Task " + taskId + ", approved by Admin: " + getCurrentUserId());
        return response;
    }

    @Transactional
    public TaskResponse rejectTask(UUID taskId) {
        TaskResponse response = taskService.rejectTask(taskId);
        log(userService.getUserById(getCurrentUserId()), taskId, "Task Rejected",
                "Task " + taskId + ", rejected by Admin: " + getCurrentUserId());
        return response;
    }

    @Transactional
    public TaskResponse reassignTask(UUID taskId, UUID newWorkerId) {
        Task task = taskService.getTaskById(taskId);
        if (task.getStatus() == TaskStatus.COMPLETED || task.getStatus() == TaskStatus.PAYMENT_RELEASED) {
            throw new IllegalStateException("Cannot Reassign Completed Tasks");
        } else if (task.getReport().getStatus() == ReportStatus.COMPLETED) {
            throw new IllegalStateException("Cannot Reassign Completed Reports");
        }
        task.setWorker(userService.getUserById(newWorkerId));
        task.setStatus(TaskStatus.ASSIGNED);
        task.setAssignedAt(Instant.now());
        taskService.saveTask(task);
        log(userService.getUserById(getCurrentUserId()), taskId, "Task Re-assigned",
                "Task " + taskId + ", Re-assigned by Admin: " + getCurrentUserId() + " to " + newWorkerId);
        return TaskResponse.mapTask(task);
    }

    /*
     * UTILITY FUNCTIONS
     */

    public void log(User admin, UUID targetId, String action, String note) {
        AdminActionLog log = new AdminActionLog();
        log.setAction(action);
        log.setAdmin(admin);
        log.setNote(note);
        log.setTargetId(targetId);
        log.setTimestamp(Instant.now());
        adminRepository.save(log);
    }

    private UUID getCurrentUserId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();

        if (!(auth.getPrincipal() instanceof CustomUserDetails user)) {
            throw new IllegalStateException("Invalid authentication context");
        }

        return user.getId();
    }

}
