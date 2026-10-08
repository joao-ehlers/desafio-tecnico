package com.irrah.desafio_tecnico.queue;

import com.irrah.desafio_tecnico.message.Message;
import com.irrah.desafio_tecnico.message.MessageRepository;
import com.irrah.desafio_tecnico.message.StatusType;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.util.List;

@RequiredArgsConstructor
@Service
public class QueueRecoveryService {
    private final InMemoryMessageQueue memoryMessageQueue;
    private final MessageRepository messageRepository;

    public void recoverQueuedMessages(){
        Long lastId = 0L;

        while(true){

            List<Message> batch = messageRepository.findRecoveryBatch(
                    lastId,
                    StatusType.QUEUED,
                    PageRequest.of(0, 100)
            );

            if(batch.isEmpty()){
                return;
            }

            for(Message message : batch){
                memoryMessageQueue.enqueue(message.getId(), message.getPriority());
            }

            lastId = batch.getLast().getId();

        }
    }

}
