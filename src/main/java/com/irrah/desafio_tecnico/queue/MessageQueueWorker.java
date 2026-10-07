package com.irrah.desafio_tecnico.queue;

import com.irrah.desafio_tecnico.message.MessageProcessingService;
import com.irrah.desafio_tecnico.message.MessageRetryService;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@RequiredArgsConstructor
@Component
public class MessageQueueWorker {

    private final MessageProcessingService messageProcessingService;
    private final MessageRetryService messageRetryService;

    @Scheduled(fixedDelayString = "${queue.worker.delay-ms:500}")
    public void processQueue(){
        messageRetryService.enqueueDueRetries();
        messageProcessingService.processPendingMessages();
    }
}
