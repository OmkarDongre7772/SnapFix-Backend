package com.snapfix.user.entity;

import java.util.List;
import java.util.UUID;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
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
@Table(name = "worker_profiles")
public class WorkerProfile {
    @Id
    private UUID userId;

    @OneToOne
    @MapsId
    @JoinColumn(name = "user_id")
    private User user;
    @Column(nullable = false)
    private String name;

    @ElementCollection
@CollectionTable(name = "worker_skills", joinColumns = @JoinColumn(name = "worker_id"))
@Column(name = "skill")
    private List<String> skills;
    @Column(nullable = false)
    private Double rating = 0.0;
}
