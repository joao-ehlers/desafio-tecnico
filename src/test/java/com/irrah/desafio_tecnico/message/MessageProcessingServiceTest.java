package com.irrah.desafio_tecnico.message;

import com.irrah.desafio_tecnico.queue.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MessageProcessingServiceTest {
    private InMemoryMessageQueue queue;
    private QueueMetrics metrics;
    private MessageProcessingService service;
    @Mock SingleMessageProcessingService processor;

    @BeforeEach void setUp() {
        queue = new InMemoryMessageQueue();
        metrics = new QueueMetrics();
        service = new MessageProcessingService(queue, processor, metrics);
    }

    @Test void shouldDoNothingWhenQueueIsEmpty() {
        service.processPendingMessages();
        verifyNoInteractions(processor);
        assertThat(metrics.snapshot().processed()).isZero();
    }

    @Test void shouldCountOnlySuccessAndFinalFailure() {
        for (long id = 1; id <= 3; id++) queue.enqueue(id, PriorityType.NORMAL);
        when(processor.process(1L)).thenReturn(ProcessingResult.RETRY_SCHEDULED);
        when(processor.process(2L)).thenReturn(ProcessingResult.SENT);
        when(processor.process(3L)).thenReturn(ProcessingResult.FAILED);
        service.processPendingMessages();
        var snapshot = metrics.snapshot();
        assertThat(snapshot.sent()).isEqualTo(1);
        assertThat(snapshot.failed()).isEqualTo(1);
        assertThat(snapshot.processed()).isEqualTo(2);
        assertThat(queue.size()).isZero();
    }

    @Test void shouldLimitEachRunToOneHundredAttempts() {
        for (long id = 1; id <= 101; id++) queue.enqueue(id, PriorityType.NORMAL);
        when(processor.process(anyLong())).thenReturn(ProcessingResult.SENT);
        service.processPendingMessages();
        verify(processor, times(100)).process(anyLong());
        verify(processor, never()).process(101L);
        assertThat(queue.size()).isEqualTo(1);
        assertThat(metrics.snapshot().sent()).isEqualTo(100);
        service.processPendingMessages();
        verify(processor).process(101L);
        assertThat(queue.size()).isZero();
        assertThat(metrics.snapshot().sent()).isEqualTo(101);
    }

    @Test void shouldNotCountUnexpectedProcessingExceptionAsFinalFailure() {
        queue.enqueue(1L, PriorityType.NORMAL);
        queue.enqueue(2L, PriorityType.NORMAL);
        var failure = new IllegalStateException("unexpected error");
        when(processor.process(1L)).thenThrow(failure);
        assertThatThrownBy(service::processPendingMessages).isSameAs(failure);
        assertThat(metrics.snapshot().processed()).isZero();
        assertThat(queue.size()).isEqualTo(1);
        verify(processor, never()).process(2L);
    }
}
