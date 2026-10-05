package com.irrah.desafio_tecnico.message.dto;

import lombok.Builder;

@Builder
public record NewMessageResponse(
        Long messageId
) {
}
