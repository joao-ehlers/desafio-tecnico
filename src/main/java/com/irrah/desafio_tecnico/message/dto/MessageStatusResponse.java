package com.irrah.desafio_tecnico.message.dto;

import com.irrah.desafio_tecnico.message.StatusType;

public record MessageStatusResponse(Long messageId, StatusType status) {

}