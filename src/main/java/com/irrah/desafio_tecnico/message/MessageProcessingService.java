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

        for(int processed = 0; processed < 100; processed ++){
            Long messageId = inMemoryMessageQueue.dequeue();

            if(messageId == null){
                break;
            }

            ProcessingResult result = singleMessageProcessingService.process(messageId);

            if(result == ProcessingResult.SENT){
                queueMetrics.registerSuccess();
            }else if(result == ProcessingResult.FAILED){
                queueMetrics.registerFailure();
            }
        }
    }
}
