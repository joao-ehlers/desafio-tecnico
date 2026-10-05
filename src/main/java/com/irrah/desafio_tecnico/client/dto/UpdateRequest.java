package com.irrah.desafio_tecnico.client.dto;

import com.irrah.desafio_tecnico.client.DocumentType;
import com.irrah.desafio_tecnico.client.PlanType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Builder;

@Builder
public record UpdateRequest(
        @NotBlank String name,
        @NotBlank String documentId,
        @NotNull DocumentType documentType
) {
}
