package com.king.orderflow.domain.order.dto;

import java.math.BigDecimal;

public record PriceLevel(
        BigDecimal price,
        BigDecimal totalQuantity
) {}
