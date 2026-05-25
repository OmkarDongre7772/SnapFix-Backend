package com.snapfix.rating.repository;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.snapfix.rating.entity.Rating;

@Repository
public interface RatingRepository extends JpaRepository<Rating, UUID>{

    List<Rating> findAllByWorkerId(UUID id);

    @Query("SELECT COALESCE(AVG(r.score), 0) FROM Rating r WHERE r.workerId = :workerId")
    double findAverageScoreByWorkerId(@Param("workerId") UUID workerId);

    boolean existsByTask_Id(UUID taskId);
    
}
