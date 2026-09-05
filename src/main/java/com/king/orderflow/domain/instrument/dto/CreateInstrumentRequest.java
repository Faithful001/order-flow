package com.king.orderflow.domain.instrument.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;

public record CreateInstrumentRequest(
        @NotBlank String symbol,
        @NotBlank String baseCurrency,
        @NotBlank String quoteCurrency,
        @NotNull @Positive BigDecimal tickSize,
        @NotNull @Positive BigDecimal minQuantity
) {}
