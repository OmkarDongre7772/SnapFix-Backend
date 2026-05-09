package com.snapfix.common.exception;

import lombok.Getter;
import lombok.Setter;

@Getter@Setter
public class ApiError {
    public ApiError(String string, int i, long timeMillis) {
        this.message = string;
        this.status = i;
        this.timestamp = timeMillis;
    }
    private String message;
    private int status;
    private long timestamp;
}
