package com.irrah.desafio_tecnico.message.dto;

import com.irrah.desafio_tecnico.message.ChannelType;
import com.irrah.desafio_tecnico.message.Message;
import com.irrah.desafio_tecnico.message.PriorityType;
import com.irrah.desafio_tecnico.message.StatusType;
import java.math.BigDecimal;
import java.time.Instant;

public record MessageResponse(
        Long id, Long conversationId, Long senderId, Long recipientId,
        String content, Instant timestamp, PriorityType priority,
        StatusType status, BigDecimal cost, ChannelType channel
) {
    public static MessageResponse from(Message message) {
        return new MessageResponse(
                message.getId(), message.getConversation().getId(),
                message.getSender().getId(),
                message.getConversation().getRecipient().getId(),
                message.getContent(), message.getTimestamp(),
                message.getPriority(), message.getStatus(),
                message.getCost(), message.getChannel());
    }
}