package com.irrah.desafio_tecnico.queue;

import com.irrah.desafio_tecnico.message.*;
import org.junit.jupiter.api.Test;
import static org.mockito.Mockito.*;


class MessageQueueWorkerTest {

    @Test void shouldRequeueDueRetriesBeforeConsuming() {
        QueueReadiness readiness = new QueueReadiness();
        readiness.markReady();

        MessageProcessingService processing = mock(MessageProcessingService.class);
        MessageRetryService retries = mock(MessageRetryService.class);
        MessageQueueWorker worker = new MessageQueueWorker(processing, retries, readiness);
        worker.processQueue();
        var order = inOrder(retries, processing);
        order.verify(retries).enqueueDueRetries();
        order.verify(processing).processPendingMessages();
        order.verifyNoMoreInteractions();
    }

    @Test
    void shouldNotProcessWhileQueueIsNotReady() {
        QueueReadiness readiness = new QueueReadiness();

        MessageProcessingService processing =
                mock(MessageProcessingService.class);
        MessageRetryService retries =
                mock(MessageRetryService.class);

        MessageQueueWorker worker =
                new MessageQueueWorker(processing, retries, readiness);

        worker.processQueue();

        verifyNoInteractions(processing, retries);
    }
}
