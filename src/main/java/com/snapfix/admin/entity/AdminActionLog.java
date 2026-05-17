package com.snapfix.admin.entity;

import java.time.Instant;
import java.util.UUID;

import com.snapfix.user.entity.User;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import lombok.Getter;
import lombok.Setter;

@Entity
@Getter
@Setter
public class AdminActionLog {


    
    @Id
    @GeneratedValue
    private UUID id;
     
    @ManyToOne
    @JoinColumn(name = "user_id", nullable = false)
    private User admin;

    private String action = "";

    private UUID targetId;

    private String note = "";

    private Instant timestamp;

    
}
