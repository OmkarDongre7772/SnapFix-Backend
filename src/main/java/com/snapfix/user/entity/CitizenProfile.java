package com.snapfix.user.entity;

import java.util.UUID;

import com.snapfix.common.entity.Location;

import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.MapsId;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "citizen_profiles")
public class CitizenProfile {
    @Id
    private UUID userId;

    @OneToOne
    @MapsId
    @JoinColumn(name = "user_id")
    private User user;
    @Column(nullable = false)
    private String name;

    @Embedded
    private Location location;
    @Column(nullable = false)
    private int reportsSubmitted = 0;
}
