package com.snapfix.user.repository;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.snapfix.user.entity.User;
import com.snapfix.user.entity.WorkerProfile;

public interface WorkerProfileRepository extends JpaRepository<WorkerProfile, UUID>{

    Optional<WorkerProfile> findByUser(User user);
    
}
