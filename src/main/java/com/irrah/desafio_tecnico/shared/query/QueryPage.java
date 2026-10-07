package com.irrah.desafio_tecnico.shared.query;

import com.irrah.desafio_tecnico.shared.exception.InvalidInputException;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;

public final class QueryPage {
    private QueryPage() {}

    public static PageRequest of(int page, int size, Sort sort) {
        if (page < 0) {
            throw new InvalidInputException("a página deve ser maior ou igual a zero");
        }
        if (size < 1 || size > 100) {
            throw new InvalidInputException("o tamanho da página deve estar entre 1 e 100");
        }
        return PageRequest.of(page, size, sort);
    }
}