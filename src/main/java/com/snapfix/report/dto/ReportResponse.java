package com.snapfix.report.dto;

import java.time.Instant;
import java.util.UUID;


import com.snapfix.report.entity.Category;
import com.snapfix.report.entity.Report;
import com.snapfix.report.entity.ReportStatus;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ReportResponse {

    private UUID id;

    private UUID citizenId;

    private String imageUrl;

    private String description;

    private Category category;

    private double lat;
    private double lng;

    private ReportStatus status;

    private int supportCount;

    private Instant createdAt;

    private String message;

    public ReportResponse(Report report) {
        this.id = report.getId();
        this.citizenId = report.getCitizenId();
        this.category = report.getCategory();
        this.description = report.getDescription();
        this.lat = report.getLocation().getY();
        this.lng = report.getLocation().getX();
        this.imageUrl = report.getImageUrl();
        this.status = report.getStatus();
        this.supportCount = report.getSupportCount();
        this.createdAt = report.getCreatedAt();
    }

    public ReportResponse() {
    }
}
