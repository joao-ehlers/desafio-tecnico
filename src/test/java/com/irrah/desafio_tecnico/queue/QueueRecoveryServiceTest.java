package com.irrah.desafio_tecnico.queue;

import com.irrah.desafio_tecnico.message.Message;
import com.irrah.desafio_tecnico.message.MessageRepository;
import com.irrah.desafio_tecnico.message.PriorityType;
import com.irrah.desafio_tecnico.message.StatusType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.data.domain.PageRequest;

import java.util.List;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class QueueRecoveryServiceTest {

    @Mock
    private MessageRepository repository;

    @Mock
    private InMemoryMessageQueue queue;

    private QueueRecoveryService service;

    @BeforeEach
    void setUp() {
        service = new QueueRecoveryService(queue, repository);
    }

    @Test
    void shouldDoNothingWhenThereAreNoQueuedMessages() {
        when(repository.findRecoveryBatch(
                0L,
                StatusType.QUEUED,
                PageRequest.of(0, 100)
        )).thenReturn(List.of());

        service.recoverQueuedMessages();

        verifyNoInteractions(queue);
    }

    @Test
    void shouldRecoverAllBatchesUsingLastIdAndOriginalPriority() {
        Message first = message(5L, PriorityType.NORMAL);
        Message second = message(12L, PriorityType.URGENT);
        Message third = message(30L, PriorityType.NORMAL);

        var page = PageRequest.of(0, 100);

        when(repository.findRecoveryBatch(
                0L, StatusType.QUEUED, page
        )).thenReturn(List.of(first, second));

        when(repository.findRecoveryBatch(
                12L, StatusType.QUEUED, page
        )).thenReturn(List.of(third));

        when(repository.findRecoveryBatch(
                30L, StatusType.QUEUED, page
        )).thenReturn(List.of());

        service.recoverQueuedMessages();

        var order = inOrder(repository, queue);

        order.verify(repository)
                .findRecoveryBatch(0L, StatusType.QUEUED, page);
        order.verify(queue).enqueue(5L, PriorityType.NORMAL);
        order.verify(queue).enqueue(12L, PriorityType.URGENT);

        order.verify(repository)
                .findRecoveryBatch(12L, StatusType.QUEUED, page);
        order.verify(queue).enqueue(30L, PriorityType.NORMAL);

        order.verify(repository)
                .findRecoveryBatch(30L, StatusType.QUEUED, page);

        order.verifyNoMoreInteractions();
    }

    @Test
    void shouldPropagateDatabaseFailure() {
        var failure = new DataAccessResourceFailureException(
                "database unavailable"
        );

        when(repository.findRecoveryBatch(
                0L,
                StatusType.QUEUED,
                PageRequest.of(0, 100)
        )).thenThrow(failure);

        assertThatThrownBy(service::recoverQueuedMessages)
                .isSameAs(failure);

        verifyNoInteractions(queue);
    }

    private Message message(Long id, PriorityType priority) {
        Message message = mock(Message.class);
        when(message.getId()).thenReturn(id);
        when(message.getPriority()).thenReturn(priority);
        return message;
    }
}