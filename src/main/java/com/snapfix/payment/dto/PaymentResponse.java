package com.snapfix.payment.dto;

import com.snapfix.payment.entity.Payment;
import com.snapfix.task.dto.TaskResponse;
import com.snapfix.user.dto.UserResponse;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class PaymentResponse {

    private String id;

    private TaskResponse task;

    private UserResponse worker;

    private String amount;

    private String status;

    private String releasedAt;

    public static PaymentResponse mapToResponse(Payment p) {
        if (p == null)
            return null;
        PaymentResponse response = new PaymentResponse();
        response.setId(p.getId().toString());
        response.setReleasedAt(p.getReleasedAt() == null ? "" : p.getReleasedAt().toString());
        response.setStatus(p.getStatus().toString());
        response.setTask(TaskResponse.mapTask(p.getTask()));
        response.setWorker(UserResponse.mapToResponse(p.getWorker()));
        response.setAmount(p.getAmount().toString());

        return response;
    }
}
