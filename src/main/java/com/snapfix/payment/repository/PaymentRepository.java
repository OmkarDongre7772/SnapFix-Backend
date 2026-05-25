package com.snapfix.payment.repository;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.snapfix.payment.entity.Payment;

@Repository
public interface PaymentRepository extends JpaRepository<Payment, UUID>{

    List<Payment> findAllByWorker_Id(UUID currentUserId);

    Payment findByTask_Id(UUID taskId);
    
}
