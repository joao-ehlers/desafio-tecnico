package com.irrah.desafio_tecnico.conversation.dto;

import java.time.Instant;

public record ConversationResponse(
        Long id, Long clientId, Long recipientId, String recipientName,
        String lastMessageContent, Instant lastMessageTime, long unreadCount
) {}