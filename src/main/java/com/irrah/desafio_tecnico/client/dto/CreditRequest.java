package com.irrah.desafio_tecnico.client.dto;

import lombok.Builder;

import java.math.BigDecimal;

@Builder
public record CreditRequest(BigDecimal amount) {
}
