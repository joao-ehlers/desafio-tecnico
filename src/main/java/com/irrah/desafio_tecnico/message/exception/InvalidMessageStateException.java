package com.irrah.desafio_tecnico.message.exception;

public class InvalidMessageStateException extends RuntimeException {

    public InvalidMessageStateException(String message) {
        super(message);
    }
}