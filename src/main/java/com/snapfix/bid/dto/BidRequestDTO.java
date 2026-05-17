package com.snapfix.bid.dto;

import java.math.BigDecimal;
import java.util.UUID;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.Getter;

@Getter
public class BidRequestDTO {
    
    @NotNull
    private UUID reportId;

    @NotNull
    @PositiveOrZero
    private BigDecimal bidAmount = BigDecimal.ZERO;

    @NotNull
    @PositiveOrZero
    private int durationEstimate = 0;

    private String resourceNote = "";
    
}
