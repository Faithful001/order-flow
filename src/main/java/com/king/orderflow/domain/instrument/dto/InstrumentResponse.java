package com.king.orderflow.domain.instrument.dto;

import com.king.orderflow.domain.instrument.enums.InstrumentStatus;

import java.math.BigDecimal;

public record InstrumentResponse(
        String symbol,
        String baseCurrency,
        String quoteCurrency,
        BigDecimal tickSize,
        BigDecimal minQuantity,
        InstrumentStatus status
) {}
