package com.snapfix.report.dto;

import com.snapfix.report.entity.Category;
import lombok.Getter;
import lombok.Setter;

import jakarta.validation.constraints.*;

@Getter
@Setter
public class ReportRequest {

    @NotBlank
    @Size(max = 1000)
    private String description;

    @NotNull
    private Category category;

    @NotNull
    @DecimalMin(value = "-90.0")
    @DecimalMax(value = "90.0")
    private Double lat;

    @NotNull
    @DecimalMin(value = "-180.0")
    @DecimalMax(value = "180.0")
    private Double lng;
}