package com.snapfix.user.repository;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.snapfix.user.entity.CitizenProfile;
import com.snapfix.user.entity.User;

public interface CitizenProfileRepository extends JpaRepository<CitizenProfile, UUID> {

    Optional<CitizenProfile> findByUser(User user);
    
}
