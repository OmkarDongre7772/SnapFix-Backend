package com.snapfix.admin.dto;

import java.util.UUID;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ReassignTaskRequest {
    private UUID newWorkerId;
}
