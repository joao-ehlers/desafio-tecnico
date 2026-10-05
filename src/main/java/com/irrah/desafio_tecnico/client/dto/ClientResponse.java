package com.irrah.desafio_tecnico.client.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Builder;

import java.math.BigDecimal;

@Builder
public record ClientResponse(
       @NotNull Long id,
       @NotBlank String name,
       @NotBlank   String documentId,
       @NotBlank   String documentType,
       @NotBlank   String planType,
       @NotNull   BigDecimal balance,
       @NotNull   BigDecimal limit,
       @NotNull   boolean active
) {
}
