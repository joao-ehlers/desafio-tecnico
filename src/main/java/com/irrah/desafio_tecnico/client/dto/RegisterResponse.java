package com.irrah.desafio_tecnico.client.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Builder;

@Builder
public record RegisterResponse(
        @NotNull Long clientId
) {
}
