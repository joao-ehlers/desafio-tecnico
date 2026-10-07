package com.irrah.desafio_tecnico.client.exception;

public class ClientNotActiveException extends RuntimeException {

    public ClientNotActiveException(String message) {
        super(message);
    }
}