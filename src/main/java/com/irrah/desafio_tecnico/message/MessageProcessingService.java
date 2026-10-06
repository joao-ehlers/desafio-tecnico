package com.irrah.desafio_tecnico.message;

import com.irrah.desafio_tecnico.queue.InMemoryMessageQueue;
import com.irrah.desafio_tecnico.message.exception.MessageNotFoundException;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@RequiredArgsConstructor
@Service
public class MessageProcessingService {

    private final InMemoryMessageQueue inMemoryMessageQueue;
    private final SingleMessageProcessingService singleMessageProcessingService;

    public void processPendingMessages(){
        Long messageId;

        while((messageId = inMemoryMessageQueue.dequeue()) != null){
            singleMessageProcessingService.process(messageId);
        }
    }
}
