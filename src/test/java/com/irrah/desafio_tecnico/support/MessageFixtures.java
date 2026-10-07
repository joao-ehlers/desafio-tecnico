package com.irrah.desafio_tecnico.support;

import com.irrah.desafio_tecnico.client.*;
import com.irrah.desafio_tecnico.conversation.*;
import com.irrah.desafio_tecnico.message.*;
import java.time.Instant;

public final class MessageFixtures {
    private MessageFixtures() {}
    public static Message queued() {
        Client client = new Client("Empresa", "52998224725", DocumentType.CPF, PlanType.PREPAID);
        Recipient recipient = new Recipient("Maria", "+5544999990000");
        return new Message(new Conversation(client, recipient), client, "Olá",
                Instant.parse("2026-10-07T12:00:00Z"), PriorityType.NORMAL, ChannelType.WHATSAPP);
    }
}
