package com.irrah.desafio_tecnico.conversation.exception;

public class RecipientNotFoundException extends RuntimeException {
    public RecipientNotFoundException() {
        super("Destinatário não encontrado");
    }
}
