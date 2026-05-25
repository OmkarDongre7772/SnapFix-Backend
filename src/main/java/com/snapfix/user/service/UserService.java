package com.snapfix.user.service;

import java.util.ArrayList;
import java.util.UUID;

import org.springframework.stereotype.Service;

import com.snapfix.common.entity.Location;
import com.snapfix.common.exception.ProfileNotFoundException;
import com.snapfix.common.exception.UserNotFoundException;
import com.snapfix.user.dto.CitizenProfileDTO;
import com.snapfix.user.dto.ProfileDTO;
import com.snapfix.user.dto.ProfileUpdateDto;
import com.snapfix.user.dto.UserResponse;
import com.snapfix.user.dto.WorkerProfileDTO;
import com.snapfix.user.entity.CitizenProfile;
import com.snapfix.user.entity.Role;
import com.snapfix.user.entity.User;
import com.snapfix.user.entity.WorkerProfile;
import com.snapfix.user.repository.CitizenProfileRepository;
import com.snapfix.user.repository.UserRepository;
import com.snapfix.user.repository.WorkerProfileRepository;
import com.snapfix.wallet.dto.WalletResponse;

import jakarta.transaction.Transactional;

@Service
public class UserService {
    private final UserRepository userRepository;
    private final CitizenProfileRepository citizenProfileRepository;
    private final WorkerProfileRepository workerProfileRepository;

    public UserService(UserRepository userRepository,
            CitizenProfileRepository citizenProfileRepository,
            WorkerProfileRepository workerProfileRepository) {
        this.userRepository = userRepository;
        this.citizenProfileRepository = citizenProfileRepository;
        this.workerProfileRepository = workerProfileRepository;
    }

    public void incrementReportSubmitted(UUID userId){
        int updatedRows = citizenProfileRepository.incrementReportsSubmitted(userId);

        if(updatedRows == 0){
            throw new  IllegalArgumentException("Citizen Profile Not Found");
        }
    }

    @Transactional
    public UserResponse getCurrentUser(String email) {

        User user = userRepository.findByEmail(email).orElseThrow(() -> new UserNotFoundException("User Not Found!"));
        Role role = user.getRole();
        ProfileDTO profile = null;
        if (role == Role.CITIZEN) {
            CitizenProfile citizen = citizenProfileRepository.findByUser(user)
                    .orElseThrow(() -> new ProfileNotFoundException("Profile not found"));

            profile = buildCitizenProfileDTO(citizen);
        } else if (role == Role.WORKER) {
            WorkerProfile worker = workerProfileRepository.findByUser(user)
                    .orElseThrow(() -> new ProfileNotFoundException("Profile not Found"));

            profile = buildWorkerProfileDTO(worker);
        }
        if (profile == null) {
            throw new ProfileNotFoundException("Invalid role or profile not found");
        }

        UserResponse response = UserResponse.mapToResponse(user);
        response.setProfile(profile);
        return response;
    }

    @Transactional
    public User getCurrentUser(UUID id) {
        return getUserById(id);
    }

    public User getUserById(UUID id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new UserNotFoundException("User Not Found!"));
    }

//          DTO's [Data Transfer Objects]

    @Transactional
    public ProfileDTO updateProfile(String email, ProfileUpdateDto dto) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new UserNotFoundException("User not found"));

        Role role = user.getRole();

        if (role == Role.CITIZEN) {
            CitizenProfile citizen = citizenProfileRepository.findByUser(user)
                    .orElseThrow(() -> new ProfileNotFoundException("Citizen profile not found"));

            citizen.setName(dto.getName());

            if (dto.getLatitude() != null && dto.getLongitude() != null) {
                Location location = new Location();
                location.setLatitude(dto.getLatitude());
                location.setLongitude(dto.getLongitude());
                citizen.setLocation(location);
            }

            citizenProfileRepository.save(citizen);
            return buildCitizenProfileDTO(citizen);

        } else if (role == Role.WORKER) {
            WorkerProfile worker = workerProfileRepository.findByUser(user)
                    .orElseThrow(() -> new ProfileNotFoundException("Worker profile not found"));

            worker.setName(dto.getName());

            if (dto.getSkills() != null) {
                worker.setSkills(dto.getSkills());
            }

            workerProfileRepository.save(worker);
            return buildWorkerProfileDTO(worker);
        }

        throw new ProfileNotFoundException("Invalid role or profile not found");
    }

    public WorkerProfileDTO buildWorkerProfileDTO(WorkerProfile worker) {
        WorkerProfileDTO dto = new WorkerProfileDTO();
        dto.setRating(worker.getRating());
        dto.setName(worker.getName());
        if (worker.getSkills() != null) {
            dto.setSkills(new ArrayList<>(worker.getSkills()));
        }
        dto.setCompletedTasks(worker.getCompletedTasks());
        dto.setWallet(WalletResponse.mapToResponse(worker.getWallet()));
        dto.setAvailable(worker.isAvailable());
        if (worker.getCurrentLocation() != null) {
            dto.setLat(worker.getCurrentLocation().getY());
            dto.setLng(worker.getCurrentLocation().getX());
        }
        return dto;
    }

    public CitizenProfileDTO buildCitizenProfileDTO(CitizenProfile citizen) {
        CitizenProfileDTO dto = new CitizenProfileDTO();
        dto.setName(citizen.getName());
        dto.setLocation(citizen.getLocation());
        dto.setReportsSubmitted(citizen.getReportsSubmitted());
        return dto;

    }

}

