package com.snapfix.user.dto;

import com.snapfix.common.entity.Location;
import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
public class CitizenProfileDTO implements ProfileDTO {
    private String name;
    private Location location;
    private int reportsSubmitted;

    public CitizenProfileDTO() {}

    public CitizenProfileDTO(String name, Location location, int reportsSubmitted){
        this.name = name;
        this.location = location;
        this.reportsSubmitted = reportsSubmitted;   
    }
}