package com.irrah.desafio_tecnico.message.projection;

public interface UnreadCountView {
    Long getConversationId();
    Long getUnreadCount();
}