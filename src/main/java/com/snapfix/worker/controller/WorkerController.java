package com.snapfix.worker.controller;

import com.snapfix.payment.dto.PaymentResponse;
import com.snapfix.payment.service.PaymentService;
import com.snapfix.rating.dto.RatingRequest;
import com.snapfix.rating.dto.RatingResponse;
import com.snapfix.rating.dto.RatingSummary;
import com.snapfix.rating.service.RatingService;
import com.snapfix.wallet.service.WalletService;
// import com.snapfix.user.repository.WorkerProfileRepository;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.snapfix.worker.dto.CreateWorkerProfileRequest;
import com.snapfix.worker.dto.UpdateLocationRequest;
import com.snapfix.worker.dto.UpdateWorkerProfileRequest;
import com.snapfix.worker.service.WorkerService;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import com.snapfix.common.entity.Location;
import com.snapfix.report.dto.ReportResponse;
import com.snapfix.task.dto.TaskResponse;
import com.snapfix.task.entity.Task;
import com.snapfix.user.dto.WorkerProfileDTO;
import com.snapfix.wallet.dto.WalletResponse;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;


@RestController
@RequestMapping("/workers")
public class WorkerController {

    private final RatingService ratingService;
    private final PaymentService paymentService;
    private final WalletService walletService;
    // private final WorkerProfileRepository workerProfileRepository;
    private final WorkerService workerService;

    public WorkerController(WorkerService workerService, WalletService walletService, PaymentService paymentService, RatingService ratingService
        // ,WorkerProfileRepository workerProfileRepository
        ) {
        this.workerService = workerService;
        // this.workerProfileRepository = workerProfileRepository;
        this.walletService = walletService;
        this.paymentService = paymentService;
        this.ratingService = ratingService;
    }

    /*
     * CONTROLLERS
     */

    @PreAuthorize("hasRole('WORKER')")
    @GetMapping("/reports/nearby")
    public ResponseEntity<List<ReportResponse>> getNearByReports() {

        return ResponseEntity.ok(
                workerService.getNearbyReports());
    }

    @PreAuthorize("hasRole('WORKER')")
    @PostMapping("/profile")
    public ResponseEntity<WorkerProfileDTO> createWorkerProfile(
            @RequestBody CreateWorkerProfileRequest request) {

        return ResponseEntity.ok(
                workerService.createWorkerProfile(
                        request.getSkills(),
                        new Location(request.getLat(), request.getLng())));
    }

    @PreAuthorize("hasRole('WORKER')")
    @PutMapping("/profile")
    public ResponseEntity<WorkerProfileDTO> updateWorkerProfile(
            @RequestBody UpdateWorkerProfileRequest request) {

        return ResponseEntity.ok(
                workerService.updateWorkerProfile(
                        request.getSkills(),
                        new Location(request.getLat(), request.getLng()),
                        request.getAvailable()));
    }

    @PreAuthorize("hasRole('WORKER')")
    @GetMapping("/profile")
    public ResponseEntity<WorkerProfileDTO> getWorkerProfile() {

        return ResponseEntity.ok(
                workerService.getWorkerProfile());
    }

    @PreAuthorize("hasRole('WORKER')")
    @PostMapping("/location")
    public ResponseEntity<Void> updateWorkerLocation(
            @RequestBody UpdateLocationRequest request) {

        workerService.updateWorkerLocation(
                new Location(request.getLat(), request.getLng()));

        return ResponseEntity.ok().build();
    }

    @PreAuthorize("hasRole('WORKER')")
    @GetMapping("/tasks")
    public ResponseEntity<List<TaskResponse>> getAssignedTasks() {
        List<Task> tasks = workerService.getAssignedTasks();
        List<TaskResponse> response = new ArrayList<>();
        for (Task task : tasks) {
                response.add(TaskResponse.mapTask(task));
        }
        return ResponseEntity.ok(response);
    }

    @PreAuthorize("hasRole('WORKER')")
    @GetMapping("/wallet")
    public ResponseEntity<WalletResponse> getWorkerWalletResponse() {
        return ResponseEntity.ok(walletService.getWorkerWallet());
    }
    
    @PreAuthorize("hasRole('WORKER')")
    @GetMapping("/payments")
    public ResponseEntity<List<PaymentResponse>> getPaymentHistory() {
        return ResponseEntity.ok(paymentService.getPaymentHistory());
    }

    @PreAuthorize("hasRole('CITIZEN')")
    @PostMapping("/{workerId}/rating")
    public ResponseEntity<RatingResponse> rateWorker(@PathVariable UUID workerId,@RequestBody RatingRequest request) {
        return ResponseEntity.ok(ratingService.rateWorker(workerId, request));
    }
    
    @PreAuthorize("hasAnyRole('CITIZEN', 'WORKER', 'ADMIN')")
    @GetMapping("/{workerId}/rating")
    public ResponseEntity<RatingSummary> getRatingSummary(@PathVariable UUID workerId) {
        return ResponseEntity.ok(ratingService.getWorkeRatingSummary(workerId));
    }
}
