package com.king.orderflow.shared.exception;

public class DomainException extends RuntimeException {
    protected DomainException(String message) {
        super(message);
    }
}
