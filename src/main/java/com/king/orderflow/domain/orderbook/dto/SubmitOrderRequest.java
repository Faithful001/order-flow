package com.king.orderflow.domain.orderbook.dto;

import com.king.orderflow.domain.orderbook.enums.OrderSide;
import com.king.orderflow.domain.orderbook.enums.OrderType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;

public record SubmitOrderRequest(
        @NotBlank
        String instrument,

        @NotNull
        OrderSide side,

        @NotNull
        OrderType type,
        BigDecimal price,

        @NotNull
        @Positive
        BigDecimal quantity
) {
}