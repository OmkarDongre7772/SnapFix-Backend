package com.snapfix.admin.controller;

import com.snapfix.admin.service.AdminService;
import com.snapfix.admin.dto.ReassignTaskRequest;
import com.snapfix.bid.service.BidService;
import com.snapfix.payment.dto.PaymentResponse;
import com.snapfix.payment.service.PaymentService;
import com.snapfix.report.service.ReportService;
import com.snapfix.task.dto.TaskDetail;
import com.snapfix.task.dto.TaskResponse;
import com.snapfix.task.entity.TaskStatus;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.snapfix.bid.dto.BidResponseDTO;
import com.snapfix.report.dto.ReportResponse;
import com.snapfix.report.entity.ReportStatus;

import java.util.List;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;




@RestController
@RequestMapping("/admin")
@PreAuthorize("hasRole('ADMIN')")
public class AdminController {
    
    private final PaymentService paymentService;
    private final AdminService adminService;
    private final BidService bidService;
    private final ReportService reportService;

    AdminController(ReportService reportService, BidService bidService, AdminService adminService, PaymentService paymentService) {
        this.reportService = reportService;
        this.bidService = bidService;
        this.adminService = adminService;
        this.paymentService = paymentService;
    }

    @GetMapping("/reports")
    public ResponseEntity<Page<ReportResponse>> getAllReports(
        @RequestParam(required = false) ReportStatus status,
        Pageable pageable
    ) {
        return ResponseEntity.ok(reportService.getAllReports(status, pageable));
    }

    @GetMapping("/reports/{id}/bids")
    public ResponseEntity<List<BidResponseDTO>> getBidsForReport(@PathVariable UUID id) {
        return ResponseEntity.ok(bidService.viewBidsForReportId(id));
    }
    
    @PostMapping("/bids/{bidId}/approve")
    public ResponseEntity<Void> approveBid(@PathVariable UUID bidId) {
        adminService.approveBid(bidId);
        return ResponseEntity.ok().build();
    }
    
    @PostMapping("/bids/{bidId}/reject")
    public ResponseEntity<Void> rejectBid(@PathVariable UUID bidId) {
        adminService.rejectBid(bidId);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/tasks")
    public List<TaskResponse> getTasksByStatus(@RequestParam(required = false) TaskStatus status) {
        return adminService.getTasksByStatus(status);
    }
    
    @GetMapping("/tasks/{id}")
    public ResponseEntity<TaskDetail> getTaskDetail(@PathVariable UUID id) {
        return ResponseEntity.ok(adminService.getTaskDetail(id));
    }
    
    @PostMapping("/tasks/{taskId}/approve")
    public ResponseEntity<TaskResponse> approveTaskResponse(@PathVariable UUID taskId) {
        return ResponseEntity.ok(adminService.approveTask(taskId));
    }
    
    @PostMapping("/tasks/{taskId}/reject")
    public ResponseEntity<TaskResponse> rejectTaskResponse(@PathVariable UUID taskId) {
        return ResponseEntity.ok(adminService.rejectTask(taskId));
    }

    @PostMapping("/tasks/{taskId}/reassign")
    public ResponseEntity<TaskResponse> reassignTask(@PathVariable UUID taskId, @RequestBody ReassignTaskRequest request) {
        if (request.getNewWorkerId() == null) {
            throw new IllegalArgumentException("newWorkerId is required");
        }
        return ResponseEntity.ok(adminService.reassignTask(taskId, request.getNewWorkerId()));
    }

    @PostMapping("payments/{taskId}/release")
    public ResponseEntity<PaymentResponse> releasePayment(@PathVariable UUID taskId) {
        return ResponseEntity.ok(paymentService.releasePayment(taskId));
    }
    
    
}
