package com.snapfix.report.repository;

import java.util.List;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import com.snapfix.report.entity.Report;
import com.snapfix.report.entity.ReportStatus;

@Repository
public interface ReportRepository extends JpaRepository<Report, UUID>{

    @Query(value = """
        SELECT * FROM reports
        WHERE ST_DWithin(
            location::geography,
            ST_MakePoint(:lng, :lat)::geography,
            :radius
        )
        ORDER BY ST_Distance(
            location::geography,
            ST_MakePoint(:lng, :lat)::geography
        )
    """, nativeQuery = true)
    List<Report> findNearbyReports(double lat, double lng, double radius);

    Page<Report> findByStatus(ReportStatus status, Pageable pageable);
}