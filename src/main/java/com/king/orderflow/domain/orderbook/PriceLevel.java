package com.king.orderflow.domain.orderbook;

import java.math.BigDecimal;

public record PriceLevel(
        BigDecimal price,
        BigDecimal totalQuantity
) {}
