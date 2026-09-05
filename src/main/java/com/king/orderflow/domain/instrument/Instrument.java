package com.king.orderflow.domain.instrument;

import com.king.orderflow.domain.instrument.enums.InstrumentStatus;
import lombok.*;

import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Instrument {
    private String symbol;
    private String baseCurrency;
    private String quoteCurrency;
    private BigDecimal tickSize;
    private BigDecimal minQuantity;
    private InstrumentStatus status;
}
