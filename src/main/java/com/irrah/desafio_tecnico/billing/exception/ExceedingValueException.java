package com.irrah.desafio_tecnico.billing.exception;

public class ExceedingValueException extends RuntimeException {

    public ExceedingValueException(String message) {
        super(message);
    }
}
