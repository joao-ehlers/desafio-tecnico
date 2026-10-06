package com.irrah.desafio_tecnico.message;

import com.irrah.desafio_tecnico.queue.InMemoryMessageQueue;
import com.irrah.desafio_tecnico.queue.QueueMetrics;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@RequiredArgsConstructor
@Service
public class MessageProcessingService {

    private final InMemoryMessageQueue inMemoryMessageQueue;
    private final SingleMessageProcessingService singleMessageProcessingService;
    private final QueueMetrics queueMetrics;

    public synchronized void processPendingMessages(){
        Long messageId;

        while((messageId = inMemoryMessageQueue.dequeue()) != null){
            StatusType statusType = singleMessageProcessingService.process(messageId);

            if(statusType == StatusType.SENT){
                queueMetrics.registerSuccess();
            }else if(statusType == StatusType.FAILED){
                queueMetrics.registerFailure();
            }
        }
    }
}
