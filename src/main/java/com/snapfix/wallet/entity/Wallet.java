package com.snapfix.wallet.entity;

import java.math.BigDecimal;
import java.util.UUID;

import com.snapfix.user.entity.User;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import jakarta.validation.constraints.DecimalMin;
import lombok.Getter;
import lombok.Setter;

@Entity
@Getter
@Setter
@Table(name = "wallets",
    uniqueConstraints = @UniqueConstraint(name = "unique_wallet_per_worker", columnNames = "worker_id")
)
public class Wallet {
    @Id
    @GeneratedValue
    private UUID id;

    @OneToOne(optional = false)
    @JoinColumn(name = "worker_id", nullable = false)
    private User worker;

    @Column(nullable = false)
    @DecimalMin(value = "0.0", message = "Balance must be greater than or equal to zero.")
    private BigDecimal balance;
}
