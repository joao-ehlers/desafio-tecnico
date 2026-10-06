package com.irrah.desafio_tecnico.message;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class SimulatedMessageSender implements MessageSender{

    @Override
    public synchronized void sendMessage(Message message){
        log.info("Envio simulado: messageId={} channel={}",
                message.getId(), message.getChannel());
    }
}
