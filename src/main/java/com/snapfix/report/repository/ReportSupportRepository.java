package com.snapfix.report.repository;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.snapfix.report.entity.ReportSupport;

@Repository
public interface ReportSupportRepository extends JpaRepository<ReportSupport, UUID> {

    boolean existsByReportIdAndUserId(UUID reportId, UUID userId);
}