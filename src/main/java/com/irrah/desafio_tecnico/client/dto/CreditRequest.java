package com.irrah.desafio_tecnico.client.dto;

import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Builder;

import java.math.BigDecimal;

@Builder
public record CreditRequest(@NotNull
                            @Positive
                            @Digits(integer = 10, fraction = 2)
                            BigDecimal amount) {
}
