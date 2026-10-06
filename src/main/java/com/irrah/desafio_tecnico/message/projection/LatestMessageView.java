package com.irrah.desafio_tecnico.message.projection;

import java.time.Instant;

public interface LatestMessageView {
    Long getConversationId();
    String getContent();
    Instant getTimestamp();
}