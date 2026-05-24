package com.snapfix.verification.repository;

import java.util.UUID;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.snapfix.verification.entity.Verification;

public interface VerificationRepository extends JpaRepository<Verification, UUID>{

    boolean existsByTask_Id(UUID taskId);
    
    Optional<Verification> findByTask_Id(UUID taskId);
    
}
