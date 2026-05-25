package com.snapfix.rating.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class RatingSummary {
    private double average_score;
    private int task_count;

    public static RatingSummary mapToResponse(double average_score, int task_count){
        RatingSummary s = new RatingSummary();
        s.setAverage_score(average_score);
        s.setTask_count(task_count);
        return s;
    }
}
