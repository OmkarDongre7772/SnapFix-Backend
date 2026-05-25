package com.snapfix.user.dto;

import java.util.ArrayList;
import java.util.List;

import com.snapfix.user.entity.WorkerProfile;
import com.snapfix.wallet.dto.WalletResponse;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class WorkerProfileDTO implements ProfileDTO {

    private String name;
    private List<String> skills;
    private Double rating;
    private int completedTasks = 0;
    private WalletResponse wallet;
    private Double lat;
    private Double lng;
    private boolean available;

    /*
     * CONSTRUCTORS
     */
    public WorkerProfileDTO() {
    }

    public WorkerProfileDTO(WorkerProfile worker) {
        this.name = worker.getName();
        this.skills = worker.getSkills() == null ? new ArrayList<>() : new ArrayList<>(worker.getSkills());
        this.rating = worker.getRating();
        this.completedTasks = worker.getCompletedTasks();
        this.wallet = WalletResponse.mapToResponse(worker.getWallet());
        if (worker.getCurrentLocation() != null) {
            this.lat = worker.getCurrentLocation().getY();
            this.lng = worker.getCurrentLocation().getX();
        }
        this.available = worker.isAvailable();
    }

    public static WorkerProfileDTO mapToResponse(WorkerProfile profile) {
        WorkerProfileDTO response = new WorkerProfileDTO();
        response.setAvailable(profile.isAvailable());
        response.setCompletedTasks(profile.getCompletedTasks());
        response.setLat(profile.getCurrentLocation().getY());
        response.setLng(profile.getCurrentLocation().getX());
        response.setName(profile.getName());
        response.setRating(profile.getRating());
        response.setSkills(profile.getSkills());
        response.setWallet(WalletResponse.mapToResponse(profile.getWallet()));
        return response;
    }

}
