package com.irrah.desafio_tecnico.client.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Builder;

import java.math.BigDecimal;
import java.time.LocalDate;

@Builder
public record BalanceResponse(
       BigDecimal balance,
       BigDecimal creditLimit,
       BigDecimal monthlyConsumption,
       BigDecimal available
){
}
