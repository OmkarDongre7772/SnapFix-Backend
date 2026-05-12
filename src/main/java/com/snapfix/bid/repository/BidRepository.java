package com.snapfix.bid.repository;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.snapfix.bid.entity.Bid;



public interface BidRepository extends JpaRepository<Bid, UUID>{
    
    List<Bid> findByWorker_Id(UUID workerId);

    List<Bid> findByReport_Id(UUID reportId);

    boolean existsByReport_IdAndWorker_Id(UUID reportId, UUID workerId);
}
