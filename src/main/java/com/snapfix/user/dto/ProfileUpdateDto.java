package com.snapfix.user.dto;

import java.util.List;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ProfileUpdateDto {

    @NotBlank(message = "Name is required")
    private String name;

    // Citizen-specific fields
    private Double latitude;
    private Double longitude;

    // Worker-specific fields
    private List<String> skills;
}
