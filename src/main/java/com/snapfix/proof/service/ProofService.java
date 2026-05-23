package com.snapfix.proof.service;

import com.snapfix.task.entity.Task;
import com.snapfix.task.entity.TaskStatus;
import com.snapfix.task.service.TaskService;
import com.snapfix.user.service.UserService;

import jakarta.transaction.Transactional;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;

import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import com.snapfix.auth.security.CustomUserDetails;
import com.snapfix.geo.util.GeoUtil;
import com.snapfix.proof.dto.ProofRequest;
import com.snapfix.proof.dto.ProofResponse;
import com.snapfix.proof.entity.Proof;
import com.snapfix.proof.repository.ProofRepository;
import com.snapfix.storage.service.StorageService;

@Service
public class ProofService {

/*
    DEPENDENCY BEAN INJECTION---------------------
*/
private final TaskService taskService;
private final UserService userService;
private final ProofRepository proofRepository;
private final StorageService storageService;

    public ProofService(
        ProofRepository proofRepository, UserService userService, TaskService taskService, StorageService storageService
    ){
        this.proofRepository = proofRepository;
        this.userService = userService;
        this.taskService = taskService;
        this.storageService = storageService;
    }

/*
    SERVICE FUNCTIONS--------------------------
*/
    @Transactional
    public ProofResponse uploadProof(ProofRequest request, UUID taskId, MultipartFile image) {
        validateProofRequest(request);
        Task task = taskService.getTask(taskId);
        if(!task.getWorker().getId().equals(getCurrentUser())){
            throw new AccessDeniedException("Only the Assigned worker can upload the proof");
        }else if(!task.getStatus().equals(TaskStatus.IN_PROGRESS)){
            throw new IllegalStateException("The task status must be IN_PROGRESS before submitting proof");
        }else if(image == null || image.isEmpty()){
            throw new IllegalArgumentException("Image is required to submit proof");
        }else if(proofRepository.existsByTask_Id(taskId)){
            throw new IllegalStateException("Proof already submitted for this task");
        }
        String imageUrl;
        try {
            imageUrl = storageService.uploadImage(image);
        } catch (Exception e) {
            throw new IllegalStateException("Image upload failed: " + e.getMessage(), e.getCause());
        }
        if(imageUrl == null){
            throw new IllegalStateException("Image upload failed, URL is null");
        }
        Proof proof = new Proof();
        proof.setGpsLocation(GeoUtil.createPoint(request.getLat(), request.getLng()));
        proof.setImageUrl(imageUrl);
        proof.setRemarks(request.getRemarks());
        proof.setSubmittedAt(Instant.now());
        proof.setWorker(userService.getUserById(getCurrentUser()));
        proof.setTask(task);
        task.setStatus(TaskStatus.PROOF_SUBMITTED);
        taskService.saveTask(task);
        proofRepository.save(proof);
        return new ProofResponse().mapResponse(proof);
    }
    
    public ProofResponse getTaskProof(UUID taskId) {
        Proof proof = proofRepository.findByTask_Id(taskId)
                .orElseThrow(() -> new IllegalArgumentException("Proof not found for task"));
        ensureCanViewProof(proof);
        return new ProofResponse().mapResponse(proof);
    }

    public List<Task> getOldProofSubmitted(){
        return proofRepository.findTasksSubmittedBefore(Instant.now().minus(5, ChronoUnit.DAYS));
    }


/*
    UTILITY FUNCTIONS-------------------------------
*/
    private UUID getCurrentUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();

        if (!(auth.getPrincipal() instanceof CustomUserDetails user)) {
            throw new IllegalStateException("Invalid authentication context");
        }

        return user.getId();
    }

    private void ensureCanViewProof(Proof proof) {
        if (hasRole("ROLE_ADMIN")) {
            return;
        }

        UUID currentUserId = getCurrentUser();
        if (proof.getWorker().getId().equals(currentUserId)) {
            return;
        }
        if (proof.getTask().getReport().getCitizenId().equals(currentUserId)) {
            return;
        }

        throw new AccessDeniedException("Only the assigned worker, report citizen, or admin can view proof");
    }

    private boolean hasRole(String role) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return auth != null && auth.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .anyMatch(role::equals);
    }


    private void validateProofRequest(ProofRequest request) {
        if (request.getRemarks() == null) {
            request.setRemarks("");
        }
        if (request.getRemarks().length() > 1000) {
            throw new IllegalArgumentException("Remarks must be at most 1000 characters");
        }
        if (request.getLat() < -90 || request.getLat() > 90) {
            throw new IllegalArgumentException("Latitude must be between -90 and 90");
        }
        if (request.getLng() < -180 || request.getLng() > 180) {
            throw new IllegalArgumentException("Longitude must be between -180 and 180");
        }
    }


}
