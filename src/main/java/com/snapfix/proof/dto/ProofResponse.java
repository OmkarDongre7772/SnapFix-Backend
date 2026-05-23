package com.snapfix.proof.dto;

import java.time.Instant;
import java.util.UUID;

import com.snapfix.proof.entity.Proof;

import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
public class ProofResponse {
     
    private UUID id;

    private UUID taskId;

    private UUID workerId;

    private String imageUrl;

    private double lat;

    private double lng;

    private String remarks = "";

    private Instant submittedAt;

    public ProofResponse mapResponse(Proof proof){
        ProofResponse response = new ProofResponse();
        response.setId(proof.getId());
        response.setImageUrl(proof.getImageUrl());
        response.setLat(proof.getGpsLocation().getY());
        response.setLng(proof.getGpsLocation().getX());
        response.setRemarks(proof.getRemarks());
        response.setSubmittedAt(proof.getSubmittedAt());
        response.setTaskId(proof.getTask().getId());
        response.setWorkerId(proof.getWorker().getId());
        return response;
    }
}
