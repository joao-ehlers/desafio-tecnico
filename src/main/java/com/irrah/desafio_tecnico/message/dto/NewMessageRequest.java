package com.irrah.desafio_tecnico.message.dto;

import com.irrah.desafio_tecnico.message.ChannelType;
import com.irrah.desafio_tecnico.message.PriorityType;
import com.irrah.desafio_tecnico.message.StatusType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Builder;

import java.time.Instant;

@Builder
public record NewMessageRequest (
       Long conversationId,
       Long recipientId,
       @NotNull Long clientId,
       String recipientName,
       String recipientPhone,
       @NotBlank String content,
       @NotNull PriorityType priorityType,
       @NotNull StatusType statusType,
       @NotNull ChannelType channelType
){

}
