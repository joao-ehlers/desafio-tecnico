package com.irrah.desafio_tecnico.billing.exception;

public class WrongConsumingMonthException extends RuntimeException {

    public WrongConsumingMonthException(String message) {
        super(message);
    }
}