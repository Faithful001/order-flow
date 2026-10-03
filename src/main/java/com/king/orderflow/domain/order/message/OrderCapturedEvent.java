package com.king.orderflow.domain.order.message;

import com.king.orderflow.domain.order.enums.OrderSide;
import com.king.orderflow.domain.order.enums.OrderType;

import java.math.BigDecimal;
import java.util.UUID;

public record OrderCapturedEvent(
        UUID orderId,
        String instrument,
        OrderSide side,
        OrderType type,
        BigDecimal price,
        BigDecimal quantity) {}