package com.snapfix.user.repository;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.snapfix.user.entity.CitizenProfile;
import com.snapfix.user.entity.User;

import jakarta.transaction.Transactional;

@Repository
public interface CitizenProfileRepository extends JpaRepository<CitizenProfile, UUID> {

    Optional<CitizenProfile> findByUser(User user);
    
    @Modifying
    @Transactional
    @Query("""
            UPDATE CitizenProfile cp
            SET cp.reportsSubmitted = cp.reportsSubmitted + 1
            WHERE cp.userId = :userId
            """)
    int incrementReportsSubmitted(@Param("userId") UUID userId);
}
