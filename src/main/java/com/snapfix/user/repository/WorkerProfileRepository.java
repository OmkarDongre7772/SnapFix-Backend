package com.snapfix.user.repository;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.snapfix.user.entity.User;
import com.snapfix.user.entity.WorkerProfile;

@Repository
public interface WorkerProfileRepository extends JpaRepository<WorkerProfile, UUID>{

    Optional<WorkerProfile> findByUser(User user);

    Optional<WorkerProfile> findByUser_Id(UUID workerId);
    
}
