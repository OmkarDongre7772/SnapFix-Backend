package com.snapfix.wallet.entity;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;
import com.snapfix.payment.entity.Payment;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import jakarta.validation.constraints.DecimalMin;
import lombok.Getter;
import lombok.Setter;

@Entity
@Getter
@Setter
@Table(name = "transactions",
    uniqueConstraints = @UniqueConstraint(name = "unique_transaction_per_payment", columnNames = "payment_id")
)
public class Transaction {
    @Id
    @GeneratedValue
    private UUID id;

    @ManyToOne
    @JoinColumn(name = "wallet_id", nullable = false)
    private Wallet walletId;

    @DecimalMin(value = "0.0", message = "Amount cannot be less than zero")
    @Column(nullable = false)
    private BigDecimal amount;

    @Column(nullable = false)
    private String type; 

    @OneToOne
    @JoinColumn(name = "payment_id", nullable = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    private Payment referenceId; 

    @Column(nullable = false)
    private Instant timestamp;
}
