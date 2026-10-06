package com.irrah.desafio_tecnico.client.dto;

import com.irrah.desafio_tecnico.billing.TransactionType;
import jakarta.validation.constraints.NotNull;
import lombok.Builder;

import java.math.BigDecimal;

@Builder
public record LimitResponse(
     @NotNull BigDecimal creditLimit,
     @NotNull Long clientId) {
}
