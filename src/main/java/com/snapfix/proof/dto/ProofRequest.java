package com.snapfix.proof.dto;


import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ProofRequest {

    private double lat;

    private double lng;

    private String remarks = "";
}
