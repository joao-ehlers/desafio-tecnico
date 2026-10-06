package com.irrah.desafio_tecnico.message.exception;

public class MessageNotFoundException extends RuntimeException {
    public MessageNotFoundException() {
        super("Mensagem não encontrada");
    }
}
