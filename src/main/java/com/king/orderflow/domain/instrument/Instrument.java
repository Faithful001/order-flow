package com.king.orderflow.domain.instrument;

import com.king.orderflow.domain.instrument.enums.InstrumentStatus;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "instruments")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Instrument {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private String symbol;

    @Column(nullable = false)
    private String baseCurrency;

    @Column(nullable = false)
    private String quoteCurrency;

    @Column(nullable = false)
    private BigDecimal tickSize;

    @Column(nullable = false)
    private BigDecimal minQuantity;

    @Column(nullable = false)
    private InstrumentStatus status;

    @Column(nullable = false)
    private Instant createdAt;

    @Column(nullable = false)
    private Instant updatedAt;

    @PrePersist
    protected void onCreate() {
        updatedAt = Instant.now();
        createdAt = Instant.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = Instant.now();
    }

}
