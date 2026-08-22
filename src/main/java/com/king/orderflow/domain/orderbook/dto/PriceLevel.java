package com.king.orderflow.domain.orderbook.dto;

import java.math.BigDecimal;

public record PriceLevel(
        BigDecimal price,
        BigDecimal totalQuantity
) {
}