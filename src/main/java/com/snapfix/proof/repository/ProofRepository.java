package com.snapfix.proof.repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import com.snapfix.proof.entity.Proof;
import com.snapfix.task.entity.Task;

public interface ProofRepository extends JpaRepository<Proof, UUID>{
    Optional<Proof> findByTask_Id(UUID taskId);
    boolean existsByTask_Id(UUID taskId);

    @Query("SELECT p.task FROM Proof p WHERE p.submittedAt < :dateTime")
    List<Task> findTasksSubmittedBefore(Instant dateTime);

}
