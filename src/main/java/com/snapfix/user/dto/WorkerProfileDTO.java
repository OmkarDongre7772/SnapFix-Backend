package com.snapfix.user.dto;

import java.util.List;


import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class WorkerProfileDTO implements ProfileDTO{
    private String name;
    private List<String> skills;
    private Double rating;
}
