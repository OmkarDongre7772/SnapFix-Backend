package com.snapfix.admin.repository;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.snapfix.admin.entity.AdminActionLog;

@Repository
public interface AdminRepository extends JpaRepository<AdminActionLog, UUID>{
    
}
