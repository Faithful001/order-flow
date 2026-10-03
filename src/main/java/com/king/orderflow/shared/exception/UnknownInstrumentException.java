package com.king.orderflow.shared.exception;

public class UnknownInstrumentException extends DomainException {
    public UnknownInstrumentException(String message) {
        super(message);
    }
}
