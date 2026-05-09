package com.snapfix.report.repository;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import com.snapfix.report.entity.Report;

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
}