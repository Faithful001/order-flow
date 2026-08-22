package com.king.orderflow.domain.orderbook;

import com.king.orderflow.domain.orderbook.enums.OrderSide;
import com.king.orderflow.domain.orderbook.enums.OrderStatus;
import com.king.orderflow.domain.orderbook.enums.OrderType;
import lombok.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Order {
    private UUID id;
    private String instrument;
    private OrderSide side;
    private OrderType type;
    private BigDecimal price;
    private BigDecimal quantity;
    private BigDecimal remainingQuantity;
    private OrderStatus status;
    private Instant createdAt;
}