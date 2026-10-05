package com.irrah.desafio_tecnico.client.dto;

import jakarta.validation.constraints.NotBlank;

import lombok.Builder;

@Builder
public record AuthRequest(
        @NotBlank String documentId
){
}
