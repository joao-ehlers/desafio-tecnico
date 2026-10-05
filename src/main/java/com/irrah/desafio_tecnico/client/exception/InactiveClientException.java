package com.irrah.desafio_tecnico.client.exception;

public class InactiveClientException extends RuntimeException {
    public InactiveClientException() {
        super("Cliente inativo");
    }
}
