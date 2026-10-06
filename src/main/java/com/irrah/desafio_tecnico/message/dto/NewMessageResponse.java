package com.irrah.desafio_tecnico.message.dto;

import com.irrah.desafio_tecnico.message.StatusType;
import lombok.Builder;

@Builder
public record NewMessageResponse(
        Long messageId,
        StatusType statusType
) {
}
