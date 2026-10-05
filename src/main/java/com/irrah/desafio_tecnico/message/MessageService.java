package com.irrah.desafio_tecnico.message;

import com.irrah.desafio_tecnico.message.dto.NewMessageRequest;
import com.irrah.desafio_tecnico.message.dto.NewMessageResponse;
import com.irrah.desafio_tecnico.queue.InMemoryMessageQueue;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;


@RequiredArgsConstructor
@Service
public class MessageService {
    private final InMemoryMessageQueue inMemoryMessageQueue;
    private final MessageRegistrationService messageRegistrationService;

    public NewMessageResponse newMessage(NewMessageRequest request){
        Long messageId =  messageRegistrationService.register(request);

        inMemoryMessageQueue.enqueue(messageId);

        return NewMessageResponse.builder().messageId(messageId).build();
    }

}
