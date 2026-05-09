package com.snapfix.report.dto;

import java.time.Instant;
import java.util.UUID;


import com.snapfix.report.entity.Category;
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

    
}
