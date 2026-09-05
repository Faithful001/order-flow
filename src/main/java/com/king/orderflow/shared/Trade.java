package com.king.orderflow.shared;

import java.math.BigDecimal;
import java.util.UUID;

public record Trade(
        UUID incomingOrderId,
        UUID restingOrderId,
        BigDecimal price,
        BigDecimal quantity
) {}
