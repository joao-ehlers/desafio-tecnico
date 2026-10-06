package com.irrah.desafio_tecnico.message;

import com.irrah.desafio_tecnico.message.exception.MessageDeliveryException;
import com.irrah.desafio_tecnico.message.exception.MessageNotFoundException;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@RequiredArgsConstructor
@Service
public class SingleMessageProcessingService {
    private final MessageRepository messageRepository;
    private final MessageSender messageSender;

    @Transactional
    public void process(Long messageId){

        Message message = messageRepository.findById(messageId).orElseThrow(MessageNotFoundException::new);

        message.startProcessing();

        try{
            messageSender.sendMessage(message);
        }catch (MessageDeliveryException e){
            message.markAsFailed();
            return;
        }

        message.markAsSent();
    }
}
