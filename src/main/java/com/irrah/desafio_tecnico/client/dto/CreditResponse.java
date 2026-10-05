package com.irrah.desafio_tecnico.client.dto;

import com.irrah.desafio_tecnico.billing.TransactionType;
import lombok.Builder;

import java.math.BigDecimal;

@Builder
public record CreditResponse(
        TransactionType transactionType,
        BigDecimal balance,
        BigDecimal amount,
        Long clientId,
        Long transactionId
) {
}
