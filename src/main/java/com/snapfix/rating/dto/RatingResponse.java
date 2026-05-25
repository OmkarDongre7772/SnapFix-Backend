package com.snapfix.rating.dto;

import com.snapfix.rating.entity.Rating;
import com.snapfix.task.dto.TaskResponse;

import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
public class RatingResponse {
    private String id;

    private TaskResponse task;

    private String workerId;

    private String citizenId;

    private int score;

    private String comment;

    private String timestamp;

    public static RatingResponse mapTOResponse(Rating r){
        RatingResponse res = new RatingResponse();
        res.setCitizenId(r.getCitizenId().toString());
        res.setComment(r.getComment() == null ? "" : r.getComment());
        res.setId(r.getId().toString());
        res.setScore(r.getScore());
        res.setTask(TaskResponse.mapTask(r.getTask()));
        res.setTimestamp(r.getTimestamp().toString());
        res.setWorkerId(r.getWorkerId().toString());
        return res;
    }
}
