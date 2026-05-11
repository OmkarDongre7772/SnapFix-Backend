package com.snapfix.worker.controller;

// import com.snapfix.user.repository.WorkerProfileRepository;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.snapfix.worker.dto.CreateWorkerProfileRequest;
import com.snapfix.worker.dto.UpdateLocationRequest;
import com.snapfix.worker.dto.UpdateWorkerProfileRequest;
import com.snapfix.worker.service.WorkerService;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import com.snapfix.common.entity.Location;
import com.snapfix.report.dto.ReportResponse;
// import com.snapfix.user.dto.ProfileDTO;
import com.snapfix.user.dto.WorkerProfileDTO;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;

@RestController
@RequestMapping("/workers")
public class WorkerController {

    // private final WorkerProfileRepository workerProfileRepository;
    private final WorkerService workerService;

    public WorkerController(WorkerService workerService
        // ,WorkerProfileRepository workerProfileRepository
        ) {
        this.workerService = workerService;
        // this.workerProfileRepository = workerProfileRepository;
    }

    /*
     * CONTROLLERS
     */

    @PreAuthorize("hasRole('WORKER')")
    @GetMapping("/reports/nearby")
    public ResponseEntity<List<ReportResponse>> getNearByReports(
            @RequestParam Double lat,
            @RequestParam Double lng) {

        return ResponseEntity.ok(
                workerService.getNearbyReports(new Location(lat, lng)));
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

}
