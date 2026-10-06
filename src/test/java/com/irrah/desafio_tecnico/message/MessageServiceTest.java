package com.irrah.desafio_tecnico.message;

import com.irrah.desafio_tecnico.message.dto.NewMessageRequest;
import com.irrah.desafio_tecnico.message.exception.MessageNotFoundException;
import com.irrah.desafio_tecnico.queue.InMemoryMessageQueue;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataAccessResourceFailureException;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;


@ExtendWith(MockitoExtension.class)
class MessageServiceTest {

    private static final Long MESSAGE_ID = 42L;

    @Mock
    private InMemoryMessageQueue inMemoryMessageQueue;

    @Mock
    private MessageRegistrationService messageRegistrationService;

    @Mock
    private MessageProcessingService messageProcessingService;

    @Mock
    private MessageRepository messageRepository;

    @Mock
    private NewMessageRequest request;

    @Mock
    private Message persistedMessage;

    private MessageService messageService;

    @BeforeEach
    void setUp() {
        messageService = new MessageService(
                inMemoryMessageQueue,
                messageRegistrationService,
                messageProcessingService,
                messageRepository
        );
    }

    @ParameterizedTest
    @EnumSource(value = StatusType.class, names = {"SENT", "FAILED"})
    void shouldReturnStatusOfRegisteredMessageAfterProcessing(StatusType finalStatus) {
        when(messageRegistrationService.register(request)).thenReturn(MESSAGE_ID);
        when(messageRepository.findById(MESSAGE_ID))
                .thenReturn(Optional.of(persistedMessage));
        when(persistedMessage.getId()).thenReturn(MESSAGE_ID);
        when(persistedMessage.getStatus()).thenReturn(finalStatus);

        var response = messageService.newMessage(request);

        assertThat(response.messageId()).isEqualTo(MESSAGE_ID);
        assertThat(response.statusType()).isEqualTo(finalStatus);


        InOrder order = inOrder(
                messageRegistrationService,
                inMemoryMessageQueue,
                messageProcessingService,
                messageRepository
        );
        order.verify(messageRegistrationService).register(request);
        order.verify(inMemoryMessageQueue).enqueue(MESSAGE_ID);
        order.verify(messageProcessingService).processPendingMessages();
        order.verify(messageRepository).findById(MESSAGE_ID);
    }

    @Test
    void shouldStopBeforeEnqueueWhenRegistrationFails() {
        var failure = new IllegalArgumentException("invalid message request");
        when(messageRegistrationService.register(request)).thenThrow(failure);

        assertThatThrownBy(() -> messageService.newMessage(request))
                .isSameAs(failure);

        verifyNoInteractions(
                inMemoryMessageQueue,
                messageProcessingService,
                messageRepository
        );
    }

    @Test
    void shouldStopBeforeEnqueueWhenRegistrationReportsPersistenceFailure() {
        var failure = new DataAccessResourceFailureException("database unavailable");
        when(messageRegistrationService.register(request)).thenThrow(failure);

        assertThatThrownBy(() -> messageService.newMessage(request))
                .isSameAs(failure);

        verifyNoInteractions(
                inMemoryMessageQueue,
                messageProcessingService,
                messageRepository
        );
    }

    @Test
    void shouldStopBeforeProcessingWhenEnqueueFails() {
        when(messageRegistrationService.register(request)).thenReturn(MESSAGE_ID);
        var failure = new IllegalStateException("queue unavailable");
        doThrow(failure).when(inMemoryMessageQueue).enqueue(MESSAGE_ID);

        assertThatThrownBy(() -> messageService.newMessage(request))
                .isSameAs(failure);

        verify(messageRegistrationService).register(request);
        verifyNoInteractions(messageProcessingService, messageRepository);
    }

    @Test
    void shouldNotReturnSuccessWhenProcessingHasInfrastructureFailure() {
        when(messageRegistrationService.register(request)).thenReturn(MESSAGE_ID);
        var failure = new DataAccessResourceFailureException("database unavailable");
        doThrow(failure).when(messageProcessingService).processPendingMessages();

        assertThatThrownBy(() -> messageService.newMessage(request))
                .isSameAs(failure);

        verify(inMemoryMessageQueue).enqueue(MESSAGE_ID);
        verifyNoInteractions(messageRepository);
    }

    @Test
    void shouldRejectResponseWhenRegisteredMessageCannotBeFound() {
        when(messageRegistrationService.register(request)).thenReturn(MESSAGE_ID);
        when(messageRepository.findById(MESSAGE_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> messageService.newMessage(request))
                .isInstanceOf(MessageNotFoundException.class);

        verify(inMemoryMessageQueue).enqueue(MESSAGE_ID);
        verify(messageProcessingService).processPendingMessages();
    }
}