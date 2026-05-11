package com.snapfix.worker.service;

import com.snapfix.user.repository.WorkerProfileRepository;
import com.snapfix.user.repository.UserRepository;
import java.util.List;
import java.util.UUID;
import java.math.BigDecimal;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import com.snapfix.auth.security.CustomUserDetails;
import com.snapfix.common.entity.Location;
import com.snapfix.report.dto.ReportResponse;
import com.snapfix.report.service.ReportService;
import com.snapfix.user.dto.WorkerProfileDTO;
import com.snapfix.user.entity.User;
import com.snapfix.user.entity.WorkerProfile;

import jakarta.transaction.Transactional;

@Service
public class WorkerService {

    private final WorkerProfileRepository workerProfileRepository;
    private final UserRepository userRepository;
    private final ReportService reportService;

    public WorkerService(ReportService reportService,
                         WorkerProfileRepository workerProfileRepository,
                         UserRepository userRepository) {
        this.reportService = reportService;
        this.workerProfileRepository = workerProfileRepository;
        this.userRepository = userRepository;
    }

    // Utilizes Report Service getNearbyReports to get all the nearby reports from an point
    public List<ReportResponse> getNearbyReports(Location location) {
        List<ReportResponse> response = reportService.getNearbyReports(location.getLatitude(), location.getLongitude(), 5000);
        
        // AUTO SCALING TO 10km if less than 5 reports in 5km
        if(response.size() < 5){
            response = reportService.getNearbyReports(location.getLatitude(), location.getLongitude(), 10000);
        }
        
        return response;
    }

    // Worker Profile Creation
    @Transactional
    public WorkerProfileDTO createWorkerProfile(List<String> skills, Location location) {
        validateCreateRequest(skills, location);
        UUID userId = getCurrentUserId();
        if (workerProfileRepository.existsById(userId)) {
            throw new IllegalStateException("Worker profile already exists");
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        WorkerProfile worker = new WorkerProfile();
        worker.setUser(user);
        worker.setName(user.getEmail());
        worker.setSkills(skills);
        worker.setCurrentLocation(location);
        worker.setWalletBalance(BigDecimal.ZERO);
        worker.setAvailable(true);

        workerProfileRepository.save(worker);
        return new WorkerProfileDTO(worker);
    }

    // Update Worker Profile ( Skills & Availability)
    @Transactional
    public WorkerProfileDTO updateWorkerProfile(List<String> skills, Location location, Boolean available) {
        WorkerProfile worker = getCurrentWorkerProfile();
        if(available != null ){worker.setAvailable(available);}
        if (location != null) {
            validateLocation(location);
            worker.setCurrentLocation(location);
        }
        if(skills != null){worker.setSkills(skills);}

        workerProfileRepository.save(worker);
        return new WorkerProfileDTO(worker);
    }

    // Get Worker Profile 
    @Transactional
    public WorkerProfileDTO getWorkerProfile(){
        return new WorkerProfileDTO(getCurrentWorkerProfile());
    }

    // Update Worker Location
    @Transactional
    public void updateWorkerLocation(Location location){
        validateLocation(location);
        WorkerProfile worker = getCurrentWorkerProfile();
        worker.setCurrentLocation(location);
        workerProfileRepository.save(worker);
    }


/*
    UTILITY FUNCTIONS
*/

    private void validateCreateRequest(List<String> skills, Location location) {
        if (skills == null) {
            throw new IllegalArgumentException("Skills are required");
        }
        if (skills.size() > 50) {
            throw new IllegalArgumentException("You can have atmost 50 Skills");
        }
        validateLocation(location);
    }

    private void validateLocation(Location location){
        if (location == null) {
            throw new IllegalArgumentException("Location is required");
        }
        if ( location.getLatitude()  < -90 || location.getLatitude()  > 90) {
            throw new IllegalArgumentException("Latitude must be between -90 and 90");
        }
        if ( location.getLongitude() < -180 || location.getLongitude() > 180) {
            throw new IllegalArgumentException("Longitude must be between -180 and 180");
        }
    }

    private UUID getCurrentUserId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();

        if (!(auth.getPrincipal() instanceof CustomUserDetails user)) {
            throw new IllegalStateException("Invalid authentication context");
        }

        return user.getId();
    }

    private WorkerProfile getCurrentWorkerProfile() {
        return workerProfileRepository.findById(getCurrentUserId())
                .orElseThrow(() -> new IllegalArgumentException("Worker profile not found"));
    }

}
