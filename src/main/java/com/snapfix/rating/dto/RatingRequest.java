package com.snapfix.rating.dto;

import java.util.UUID;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class RatingRequest {
    private UUID taskId;
    private int score;
    private String comment;
}
