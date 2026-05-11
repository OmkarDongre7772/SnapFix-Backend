package com.snapfix.user.dto;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import com.snapfix.common.entity.Location;
import com.snapfix.user.entity.WorkerProfile;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class WorkerProfileDTO implements ProfileDTO{
    
    private String name;
    private List<String> skills;
    private Double rating;
    private int completedTasks = 0;
    private BigDecimal walletBalance;
    private Location currentLocation;
    private boolean available;

/*
    CONSTRUCTORS
*/
    public WorkerProfileDTO() {
    }
    public WorkerProfileDTO(WorkerProfile worker) {
        this.name = worker.getName();
        this.skills = new ArrayList<>(worker.getSkills());
        this.rating = worker.getRating();
        this.completedTasks = worker.getCompletedTasks();
        this.walletBalance = worker.getWalletBalance();
        this.currentLocation = worker.getCurrentLocation();
        this.available = worker.isAvailable();
    }

    
}
