package com.snapfix.bid.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import com.snapfix.bid.entity.Bid;
import com.snapfix.bid.entity.BidStatus;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class BidResponseDTO {

    UUID id;
    UUID reportId;
    UUID workerId;
    String workerEmail;
    BigDecimal bidAmount;
    Integer durationEstimate;
    String resourceNote;
    BidStatus status;
    Instant createdAt;

    public BidResponseDTO(Bid bid) {
        this.id = bid.getId();
        this.reportId = bid.getReport().getId();
        this.workerId = bid.getWorker().getId();
        this.workerEmail = bid.getWorker().getEmail();
        this.bidAmount = bid.getBidAmount();
        this.durationEstimate = bid.getDurationEstimate();
        this.resourceNote = bid.getResourceNote();
        this.status = bid.getStatus();
        this.createdAt = bid.getCreatedAt();
    }

    public BidResponseDTO() {
    }

}
