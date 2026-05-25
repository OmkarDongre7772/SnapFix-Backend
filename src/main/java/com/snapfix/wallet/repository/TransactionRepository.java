package com.snapfix.wallet.repository;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.snapfix.wallet.entity.Transaction;

@Repository
public interface TransactionRepository extends JpaRepository< Transaction, UUID >{
    
}
