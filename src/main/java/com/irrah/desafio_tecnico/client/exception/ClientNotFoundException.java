package com.irrah.desafio_tecnico.client.exception;

public class ClientNotFoundException extends RuntimeException {
    public ClientNotFoundException() {
        super("Cliente não cadastrado");
    }
}
