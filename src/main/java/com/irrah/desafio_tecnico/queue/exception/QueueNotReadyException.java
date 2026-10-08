package com.irrah.desafio_tecnico.queue.exception;

public class QueueNotReadyException extends RuntimeException {
    public QueueNotReadyException() {
        super("A fila está sendo inicializada. Tente novamente em instantes.");
    }
}
