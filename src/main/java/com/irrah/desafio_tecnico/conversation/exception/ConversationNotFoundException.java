package com.irrah.desafio_tecnico.conversation.exception;

public class ConversationNotFoundException extends RuntimeException {
    public ConversationNotFoundException() {
        super("Conversa não encontrada para este cliente");
    }
}
