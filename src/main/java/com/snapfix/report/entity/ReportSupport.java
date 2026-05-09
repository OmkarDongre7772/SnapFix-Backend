package com.snapfix.report.entity;

import java.time.Instant;
import java.util.UUID;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "report_support",
    uniqueConstraints =     @UniqueConstraint(columnNames = {"report_id", "user_id"})
)
@Getter
@Setter
public class ReportSupport {
    @Id
    @GeneratedValue
    private UUID id;
    private UUID reportId;
    private UUID userId;
    private Instant createdAt;
}
