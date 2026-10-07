package com.irrah.desafio_tecnico.message;

import com.irrah.desafio_tecnico.queue.InMemoryMessageQueue;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageRequest;
import java.time.*;
import java.util.List;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MessageRetryServiceTest {
    private static final Instant NOW = Instant.parse("2026-10-07T12:00:00Z");
    @Mock MessageRepository repository;
    @Mock MessageStateService stateService;
    @Mock InMemoryMessageQueue queue;
    @Mock Message selected;
    @Mock Message requeued;
    private MessageRetryService service;

    @BeforeEach void setUp() {
        service = new MessageRetryService(repository, stateService, queue,
                Clock.fixed(NOW, ZoneOffset.UTC));
    }

    @Test void shouldDoNothingWithoutDueRetries() {
        when(repository.findAllFailedAndNextAttemptEqualOrLessThan(
                NOW, StatusType.FAILED, PageRequest.of(0, 100))).thenReturn(List.of());
        service.enqueueDueRetries();
        verifyNoInteractions(stateService, queue);
    }

    @Test void shouldTransitionBeforeEnqueuingWithOriginalPriority() {
        when(repository.findAllFailedAndNextAttemptEqualOrLessThan(
                NOW, StatusType.FAILED, PageRequest.of(0, 100))).thenReturn(List.of(selected));
        when(selected.getId()).thenReturn(42L);
        when(stateService.queueForRetry(42L)).thenReturn(requeued);
        when(requeued.getId()).thenReturn(42L);
        when(requeued.getPriority()).thenReturn(PriorityType.URGENT);
        service.enqueueDueRetries();
        var order = inOrder(stateService, queue);
        order.verify(stateService).queueForRetry(42L);
        order.verify(queue).enqueue(42L, PriorityType.URGENT);
        order.verifyNoMoreInteractions();
    }

    @Test void shouldNotEnqueueIfStateTransitionFails() {
        when(repository.findAllFailedAndNextAttemptEqualOrLessThan(
                NOW, StatusType.FAILED, PageRequest.of(0, 100))).thenReturn(List.of(selected));
        when(selected.getId()).thenReturn(42L);
        var failure = new IllegalStateException("not eligible");
        when(stateService.queueForRetry(42L)).thenThrow(failure);
        assertThatThrownBy(service::enqueueDueRetries).isSameAs(failure);
        verifyNoInteractions(queue);
    }
}
