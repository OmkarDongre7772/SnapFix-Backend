package com.snapfix.user.dto;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import com.snapfix.user.entity.WorkerProfile;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class WorkerProfileDTO implements ProfileDTO {

    private String name;
    private List<String> skills;
    private Double rating;
    private int completedTasks = 0;
    private BigDecimal walletBalance;
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
        this.walletBalance = worker.getWalletBalance();
        if (worker.getCurrentLocation() != null) {
            this.lat = worker.getCurrentLocation().getY();
            this.lng = worker.getCurrentLocation().getX();
        }
        this.available = worker.isAvailable();
    }

}
