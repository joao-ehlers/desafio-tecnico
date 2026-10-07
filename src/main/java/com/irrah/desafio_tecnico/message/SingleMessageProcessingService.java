package com.irrah.desafio_tecnico.message;

import com.irrah.desafio_tecnico.message.exception.MessageDeliveryException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@RequiredArgsConstructor
@Service
public class SingleMessageProcessingService {
    private final MessageSender messageSender;
    private final MessageStateService messageStateService;

    public ProcessingResult process(Long messageId){
        Message message = messageStateService.startProcessing(messageId);

        try{
            messageSender.sendMessage(message);
        }catch (MessageDeliveryException e){
            return messageStateService.markAsFailed(message.getId());
        }

        return messageStateService.markAsSent(message.getId());
    }
}
