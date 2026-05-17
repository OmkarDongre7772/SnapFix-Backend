package com.snapfix.admin.repository;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.snapfix.admin.entity.AdminActionLog;

public interface AdminRepository extends JpaRepository<AdminActionLog, UUID>{
    
}
