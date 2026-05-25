package com.snapfix.bid.entity;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import com.snapfix.report.entity.Report;
import com.snapfix.user.entity.User;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import jakarta.validation.constraints.DecimalMin;
import lombok.Getter;
import lombok.Setter;

@Entity
@Getter
@Setter
@Table(name = "bid", uniqueConstraints = {
        @UniqueConstraint(name = "unique_bid_per_report_and_worker", columnNames = { "report_id", "worker_id" })
})
public class Bid {

    @Id
    @GeneratedValue
    private UUID id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "report_id", nullable = false)
    private Report report;

    @ManyToOne(optional = false)
    @JoinColumn(name = "worker_id", nullable = false)
    private User worker;

    @Column(nullable = false)
    @DecimalMin(value = "0.0", message = "Bidding amount must be greater than or equal to zero.")
    private BigDecimal bidAmount = BigDecimal.ZERO;

    @Column(nullable = false)
    private int durationEstimate = 0;

    @Column(nullable = false)
    private String resourceNote = "";

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private BidStatus status = BidStatus.ACTIVE;

    @Column(nullable = false)
    private Instant createdAt;

    public Bid() {
    }

    public Bid(Report report, User worker, BigDecimal bidAmount, int durationEstimate, String resourceNote) {
        this.report = report;
        this.worker = worker;
        this.bidAmount = bidAmount;
        this.durationEstimate = durationEstimate;
        this.resourceNote = resourceNote;

        this.createdAt = Instant.now();
    }
}
