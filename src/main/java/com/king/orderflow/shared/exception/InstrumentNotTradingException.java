package com.king.orderflow.shared.exception;

import com.king.orderflow.domain.instrument.Instrument;

public class InstrumentNotTradingException extends DomainException {
    public InstrumentNotTradingException(String message) {
        super(message);
    }
}
