package com.irrah.desafio_tecnico.message.dto;

import com.irrah.desafio_tecnico.message.StatusType;
import lombok.Builder;

@Builder
public record MessageStatusResponse(Long messageId, StatusType status) {

}