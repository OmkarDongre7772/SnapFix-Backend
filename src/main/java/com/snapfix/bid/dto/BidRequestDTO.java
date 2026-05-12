package com.snapfix.bid.dto;

import java.math.BigDecimal;
import java.util.UUID;


import lombok.Getter;

@Getter
public class BidRequestDTO {
    
    private UUID reportId;

    private BigDecimal bidAmount = BigDecimal.ZERO;

    private int durationEstimate = 0;

    private String resourceNote = "";   
    
}
