package com.snapfix.rating.entity;

import java.time.Instant;
import java.util.UUID;

import org.hibernate.validator.constraints.Range;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

import com.snapfix.task.entity.Task;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.Setter;

@Entity
@Getter
@Setter
@Table(name = "ratings",
    uniqueConstraints = @UniqueConstraint(name = "unique_rating_per_task", columnNames = "task_id")
)
public class Rating {
    @Id
    @GeneratedValue
    private UUID id;

    @OneToOne
    @JoinColumn(name = "task_id", nullable = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    private Task task;

    @Column(nullable = false)
    private UUID workerId;

    @Column(nullable = false)
    private UUID citizenId;

    @Column(nullable = false)
    @Range(min = 1, max = 5, message = "Rating must be between 1 and 5")
    private int score;

    @Column(nullable = false)
    private String comment = "";

    @Column(nullable = false)
    private Instant timestamp;
}
