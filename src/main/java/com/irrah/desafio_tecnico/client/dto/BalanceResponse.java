package com.irrah.desafio_tecnico.client.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Builder;

import java.math.BigDecimal;

@Builder
public record BalanceResponse(
      @NotNull BigDecimal balance
){
}
