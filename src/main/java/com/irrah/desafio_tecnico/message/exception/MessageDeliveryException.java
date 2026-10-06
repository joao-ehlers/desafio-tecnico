package com.irrah.desafio_tecnico.message.exception;

public class MessageDeliveryException extends RuntimeException {
    public MessageDeliveryException() {
        super("Não foi possivel entregar a mensagem");
    }
}
