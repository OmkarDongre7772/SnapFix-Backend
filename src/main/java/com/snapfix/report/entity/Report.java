package com.snapfix.report.entity;

import java.time.Instant;
import java.util.UUID;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import org.locationtech.jts.geom.Point;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

@Entity
@Getter
@Setter
@Table(name = "reports", indexes = {
        @Index(name = "idx_reports_category", columnList = "category"),
        @Index(name = "idx_reports_created_at", columnList = "created_at")
})
public class Report {
    public Report(){
        
    }

    public Report(Report report) {
        this.id = report.getId();
        this.citizenId = report.getCitizenId();
        this.category = report.getCategory();
        this.createdAt = report.getCreatedAt();
        this.description = report.getDescription();
        this.imageUrl = report.getImageUrl();
        this.location = report.getLocation();
        this.status = report.getStatus();
        this.supportCount = report.getSupportCount();
    }

    @Id
    @GeneratedValue
    @Column(nullable = false, updatable = false)
    private UUID id;

    @Column(nullable = false)
    private UUID citizenId;

    @Column(nullable = false)
    private String imageUrl;

    @Column(nullable = false, length = 1000)
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Category category;

    @Column(columnDefinition = "geometry(Point, 4326)")
    @JdbcTypeCode(SqlTypes.GEOMETRY)
    private Point location;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ReportStatus status;

    @Column(nullable = false)
    private int supportCount;

    @Column(nullable = false)
    private Instant createdAt;
}
