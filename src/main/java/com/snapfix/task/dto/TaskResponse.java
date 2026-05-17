package com.snapfix.task.dto;

import java.time.Instant;
import java.util.UUID;

import com.snapfix.task.entity.Task;
import com.snapfix.task.entity.TaskStatus;

import lombok.Getter;
import lombok.Setter;


@Getter
@Setter
public class TaskResponse {
    private UUID id;

    private UUID reportId;

    private UUID workerId;

    private Instant assignedAt;

    private TaskStatus status;

    private int retryCount;

/*
    STATIC MAPPING FUNCTION
*/

    public static TaskResponse mapTask(Task t){
        TaskResponse response = new TaskResponse();
        response.setId(t.getId());
        response.setAssignedAt(t.getAssignedAt());
        response.setReportId(t.getReport().getId());
        response.setRetryCount(t.getRetryCount());
        response.setStatus(t.getStatus());
        response.setWorkerId(t.getWorker().getId());
        return response;
    }
}
