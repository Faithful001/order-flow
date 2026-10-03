package com.king.orderflow.shared.exception;

public class InstrumentAlreadyExistsException extends DomainException {
    public InstrumentAlreadyExistsException(String message) {
        super(message);
    }
}
