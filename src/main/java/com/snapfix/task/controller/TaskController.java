package com.snapfix.task.controller;

import com.snapfix.proof.service.ProofService;
import com.snapfix.verification.service.VerificationService;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.snapfix.proof.dto.ProofRequest;
import com.snapfix.proof.dto.ProofResponse;
import com.snapfix.task.dto.TaskResponse;
import com.snapfix.task.service.TaskService;
import com.snapfix.verification.dto.VerificationResponse;
import com.snapfix.verification.dto.VerificationStatus;

import java.util.UUID;

import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;




@RestController
@RequestMapping("/tasks")
public class TaskController {
    
    private final VerificationService verificationService;
    private final ProofService proofService;
    TaskService taskService;

    public TaskController(TaskService taskService, ProofService proofService, VerificationService verificationService){
        this.taskService = taskService;
        this.proofService = proofService;
        this.verificationService = verificationService;
    }

    @PreAuthorize("hasRole('WORKER')")
    @GetMapping("/{id}")
    public ResponseEntity<TaskResponse> getTask(@PathVariable UUID id) {
        return ResponseEntity.ok(TaskResponse.mapTask(taskService.getTaskOfWorkerByTask_Id(id)));
    }

    @PreAuthorize("hasRole('WORKER')")
    @PatchMapping("/{id}/start")
    public ResponseEntity<Void> startTask(@PathVariable UUID id){
        taskService.startTask(id);
        return ResponseEntity.ok().build();
    }
    
    @PreAuthorize("hasRole('WORKER')")
    @PostMapping(value = "/{taskId}/proof", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ProofResponse>  uploadProof(@RequestParam Double lat, @RequestParam Double lng, @RequestParam(required = false) String remarks, @RequestPart("image") MultipartFile image, @PathVariable UUID taskId) {
        ProofRequest request = new ProofRequest();
        request.setLat(lat);
        request.setLng(lng);
        request.setRemarks(remarks);
        return ResponseEntity.ok(proofService.uploadProof(request, taskId, image));
    }

    @PreAuthorize("hasAnyRole('CITIZEN', 'WORKER', 'ADMIN')")
    @GetMapping("/{taskId}/proof")
    public ResponseEntity<ProofResponse> getTaskProof(@PathVariable UUID taskId) {
        return ResponseEntity.ok(proofService.getTaskProof(taskId));
    }
    
    @PreAuthorize("hasRole('CITIZEN')")
    @PostMapping("/{taskId}/verify")
    public ResponseEntity<VerificationResponse> verifyTask(@PathVariable UUID taskId, @RequestParam VerificationStatus status, @RequestParam(required = false) String comments){
        return ResponseEntity.ok(verificationService.verifyTask(taskId, status, comments));
    }

    @PreAuthorize("hasRole('WORKER')")
    @PostMapping("/{taskId}/retry")
    public ResponseEntity<TaskResponse> retryTask(@PathVariable UUID taskId){
        return ResponseEntity.ok(taskService.retryTask(taskId));
    }
}
