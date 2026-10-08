package com.irrah.desafio_tecnico.message;

import com.irrah.desafio_tecnico.message.dto.NewMessageRequest;
import com.irrah.desafio_tecnico.message.exception.InvalidMessageStateException;
import com.irrah.desafio_tecnico.queue.InMemoryMessageQueue;
import com.irrah.desafio_tecnico.queue.QueueReadiness;
import com.irrah.desafio_tecnico.queue.exception.QueueNotReadyException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataAccessResourceFailureException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MessageServiceTest {

    private static final Long CLIENT_ID = 7L;
    private static final Long MESSAGE_ID = 42L;

    @Mock
    private InMemoryMessageQueue inMemoryMessageQueue;

    @Mock
    private MessageRegistrationService messageRegistrationService;

    @Mock
    private MessageRepository messageRepository;

    @Mock
    private NewMessageRequest request;

    @Mock
    private Message registeredMessage;

    @Mock
    private QueueReadiness readiness;

    private MessageService messageService;


    @BeforeEach
    void setUp() {
        messageService = new MessageService(
                inMemoryMessageQueue,
                messageRegistrationService,
                messageRepository,
                readiness
        );
    }

    @ParameterizedTest
    @EnumSource(
            value = PriorityType.class,
            names = {"NORMAL", "URGENT"}
    )
    void shouldRegisterAndEnqueueMessageWithItsPriority(
            PriorityType priority
    ) {
        when(readiness.isReady()).thenReturn(true);

        stubRegistration(priority);

        var response = messageService.newMessage(CLIENT_ID, request);

        assertThat(response.messageId()).isEqualTo(MESSAGE_ID);
        assertThat(response.statusType()).isEqualTo(StatusType.QUEUED);

        InOrder order = inOrder(
                messageRegistrationService,
                inMemoryMessageQueue
        );

        order.verify(messageRegistrationService)
                .register(CLIENT_ID, request);

        order.verify(inMemoryMessageQueue)
                .enqueue(MESSAGE_ID, priority);

        order.verifyNoMoreInteractions();

        verifyNoInteractions(messageRepository);

        verifyNoInteractions(request);
    }

    @Test
    void shouldStopBeforeEnqueueWhenRegistrationFails() {
        when(readiness.isReady()).thenReturn(true);

        var failure =
                new InvalidMessageStateException("invalid message request");

        when(messageRegistrationService.register(CLIENT_ID, request))
                .thenThrow(failure);

        assertThatThrownBy(() ->
                messageService.newMessage(CLIENT_ID, request)
        ).isSameAs(failure);

        verifyNoInteractions(
                inMemoryMessageQueue,
                messageRepository
        );
    }

    @Test
    void shouldStopBeforeEnqueueWhenRegistrationReportsPersistenceFailure() {
        when(readiness.isReady()).thenReturn(true);

        var failure = new DataAccessResourceFailureException(
                "database unavailable"
        );

        when(messageRegistrationService.register(CLIENT_ID, request))
                .thenThrow(failure);

        assertThatThrownBy(() ->
                messageService.newMessage(CLIENT_ID, request)
        ).isSameAs(failure);

        verifyNoInteractions(
                inMemoryMessageQueue,
                messageRepository
        );
    }

    @Test
    void shouldPropagateEnqueueFailureWithoutRegisteringAgain() {
        when(readiness.isReady()).thenReturn(true);

        stubRegistration(PriorityType.NORMAL);

        var failure = new InvalidMessageStateException("queue unavailable");

        doThrow(failure)
                .when(inMemoryMessageQueue)
                .enqueue(MESSAGE_ID, PriorityType.NORMAL);

        assertThatThrownBy(() ->
                messageService.newMessage(CLIENT_ID, request)
        ).isSameAs(failure);

        verify(messageRegistrationService, times(1))
                .register(CLIENT_ID, request);

        verify(inMemoryMessageQueue, times(1))
                .enqueue(MESSAGE_ID, PriorityType.NORMAL);

        verifyNoInteractions(messageRepository);
    }

    private void stubRegistration(PriorityType priority) {
        when(readiness.isReady()).thenReturn(true);

        when(messageRegistrationService.register(CLIENT_ID, request))
                .thenReturn(registeredMessage);

        when(registeredMessage.getId()).thenReturn(MESSAGE_ID);
        when(registeredMessage.getPriority()).thenReturn(priority);
        when(registeredMessage.getStatus()).thenReturn(StatusType.QUEUED);
    }

    @Test
    void shouldRejectSubmissionBeforeRegistrationWhenQueueIsNotReady() {

        assertThatThrownBy(() ->
                messageService.newMessage(CLIENT_ID, request)
        ).isInstanceOf(QueueNotReadyException.class);

        verifyNoInteractions(
                messageRegistrationService,
                inMemoryMessageQueue,
                messageRepository
        );
    }
}