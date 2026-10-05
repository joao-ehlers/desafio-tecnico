package com.irrah.desafio_tecnico.client.exception;

public class DuplicateDocumentException extends RuntimeException {

    public DuplicateDocumentException() {
        super("já existe um cliente cadastrado com esse documento");
    }
}