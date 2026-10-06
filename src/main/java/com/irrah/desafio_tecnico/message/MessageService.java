package com.irrah.desafio_tecnico.message;

import com.irrah.desafio_tecnico.message.dto.NewMessageRequest;
import com.irrah.desafio_tecnico.message.dto.NewMessageResponse;
import com.irrah.desafio_tecnico.message.exception.MessageNotFoundException;
import com.irrah.desafio_tecnico.queue.InMemoryMessageQueue;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;


@RequiredArgsConstructor
@Service
public class MessageService {
    private final InMemoryMessageQueue inMemoryMessageQueue;
    private final MessageRegistrationService messageRegistrationService;
    private final MessageProcessingService messageProcessingService;
    private final MessageRepository messageRepository;

    public NewMessageResponse newMessage(NewMessageRequest request){
        Long messageId =  messageRegistrationService.register(request);

        inMemoryMessageQueue.enqueue(messageId);

        messageProcessingService.processPendingMessages();

        Message message = messageRepository.findById(messageId)
                .orElseThrow(MessageNotFoundException::new);

        return NewMessageResponse.builder().messageId(message.getId())
                .statusType(message.getStatus()).build();
    }

}
