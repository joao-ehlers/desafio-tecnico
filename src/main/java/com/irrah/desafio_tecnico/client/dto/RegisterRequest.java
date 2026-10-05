package com.irrah.desafio_tecnico.client.dto;

import com.irrah.desafio_tecnico.client.DocumentType;
import com.irrah.desafio_tecnico.client.PlanType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Builder;

import java.math.BigDecimal;

@Builder
public record RegisterRequest(
       @NotBlank String name,
       @NotBlank String documentId,
       @NotNull DocumentType documentType,
       @NotNull PlanType planType
) {
}
