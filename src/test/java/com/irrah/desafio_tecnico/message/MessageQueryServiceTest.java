package com.irrah.desafio_tecnico.message;

import com.irrah.desafio_tecnico.client.*;
import com.irrah.desafio_tecnico.client.exception.ClientNotFoundException;
import com.irrah.desafio_tecnico.conversation.*;
import com.irrah.desafio_tecnico.conversation.exception.ConversationNotFoundException;
import com.irrah.desafio_tecnico.message.exception.MessageNotFoundException;
import com.irrah.desafio_tecnico.shared.exception.InvalidInputException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.*;
import org.springframework.test.util.ReflectionTestUtils;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MessageQueryServiceTest {
    private static final Long CLIENT_ID = 1L;
    private static final Long CONVERSATION_ID = 10L;
    private static final Long MESSAGE_ID = 42L;
    private static final Instant TIME = Instant.parse("2026-10-06T00:00:00Z");

    @Mock private MessageRepository messageRepository;
    @Mock private ConversationRepository conversationRepository;
    @Mock private ClientRepository clientRepository;
    private MessageQueryService service;

    @BeforeEach
    void setUp() {
        service = new MessageQueryService(messageRepository, conversationRepository, clientRepository);
    }

    @Test
    void shouldMapOwnedMessageToResponse() {
        when(clientRepository.existsById(CLIENT_ID)).thenReturn(true);
        when(messageRepository.findOwnedMessage(MESSAGE_ID, CLIENT_ID))
                .thenReturn(Optional.of(message()));

        var result = service.getMessage(CLIENT_ID, MESSAGE_ID);

        assertThat(result.id()).isEqualTo(MESSAGE_ID);
        assertThat(result.conversationId()).isEqualTo(CONVERSATION_ID);
        assertThat(result.senderId()).isEqualTo(CLIENT_ID);
        assertThat(result.recipientId()).isEqualTo(7L);
        assertThat(result.content()).isEqualTo("Olá");
        assertThat(result.timestamp()).isEqualTo(TIME);
        assertThat(result.priority()).isEqualTo(PriorityType.NORMAL);
        assertThat(result.status()).isEqualTo(StatusType.SENT);
        assertThat(result.cost()).isEqualByComparingTo("0.25");
        assertThat(result.channel()).isEqualTo(ChannelType.WHATSAPP);
        verify(messageRepository).findOwnedMessage(MESSAGE_ID, CLIENT_ID);
        verify(messageRepository, never()).findById(anyLong());
    }

    @Test
    void shouldReturnOwnedMessageStatus() {
        when(clientRepository.existsById(CLIENT_ID)).thenReturn(true);
        when(messageRepository.findOwnedMessage(MESSAGE_ID, CLIENT_ID))
                .thenReturn(Optional.of(message()));

        var result = service.getStatus(CLIENT_ID, MESSAGE_ID);

        assertThat(result.messageId()).isEqualTo(MESSAGE_ID);
        assertThat(result.status()).isEqualTo(StatusType.SENT);
        verify(messageRepository, never()).findById(anyLong());
    }

    enum Operation { DETAIL, STATUS, LIST, HISTORY }

    @ParameterizedTest
    @EnumSource(Operation.class)
    void shouldRejectUnknownClientBeforeQueryingMessages(Operation operation) {
        when(clientRepository.existsById(CLIENT_ID)).thenReturn(false);
        assertThatThrownBy(() -> invoke(operation, CLIENT_ID))
                .isInstanceOf(ClientNotFoundException.class);
        verifyNoInteractions(messageRepository, conversationRepository);
    }

    @ParameterizedTest
    @EnumSource(Operation.class)
    void shouldRejectNullClientWithoutQueryingRepositories(Operation operation) {
        assertThatThrownBy(() -> invoke(operation, null))
                .isInstanceOf(ClientNotFoundException.class);
        verifyNoInteractions(clientRepository, messageRepository, conversationRepository);
    }

    @ParameterizedTest
    @EnumSource(value = Operation.class, names = {"DETAIL", "STATUS"})
    void shouldRejectMissingOrUnownedMessage(Operation operation) {
        when(clientRepository.existsById(CLIENT_ID)).thenReturn(true);
        when(messageRepository.findOwnedMessage(MESSAGE_ID, CLIENT_ID)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> invoke(operation, CLIENT_ID))
                .isInstanceOf(MessageNotFoundException.class);
        verify(messageRepository).findOwnedMessage(MESSAGE_ID, CLIENT_ID);
        verify(messageRepository, never()).findById(anyLong());
    }

    @Test
    void shouldForwardAllFiltersAndDescendingPagination() {
        var pageable = PageRequest.of(1, 2, Sort.by(Sort.Direction.DESC, "timestamp", "id"));
        when(clientRepository.existsById(CLIENT_ID)).thenReturn(true);
        when(conversationRepository.existsByIdAndClientId(CONVERSATION_ID, CLIENT_ID)).thenReturn(true);
        when(messageRepository.searchOwnedMessages(CLIENT_ID, CONVERSATION_ID,
                StatusType.SENT, PriorityType.NORMAL, ChannelType.WHATSAPP, pageable))
                .thenReturn(new PageImpl<>(List.of(message()), pageable, 3));

        var result = service.listMessages(CLIENT_ID, CONVERSATION_ID,
                StatusType.SENT, PriorityType.NORMAL, ChannelType.WHATSAPP, 1, 2);

        assertThat(result.content()).hasSize(1);
        assertThat(result.content().get(0).id()).isEqualTo(MESSAGE_ID);
        assertThat(result.page()).isEqualTo(1);
        assertThat(result.size()).isEqualTo(2);
        assertThat(result.totalElements()).isEqualTo(3);
        assertThat(result.totalPages()).isEqualTo(2);
        verify(messageRepository).searchOwnedMessages(CLIENT_ID, CONVERSATION_ID,
                StatusType.SENT, PriorityType.NORMAL, ChannelType.WHATSAPP, pageable);
    }

    @Test
    void shouldListWithoutOptionalFilters() {
        var pageable = PageRequest.of(0, 20, Sort.by(Sort.Direction.DESC, "timestamp", "id"));
        when(clientRepository.existsById(CLIENT_ID)).thenReturn(true);
        when(messageRepository.searchOwnedMessages(CLIENT_ID, null, null, null, null, pageable))
                .thenReturn(new PageImpl<>(List.of(message()), pageable, 1));
        var result = service.listMessages(CLIENT_ID, null, null, null, null, 0, 20);
        assertThat(result.content()).hasSize(1);
        verifyNoInteractions(conversationRepository);
    }

    @Test
    void shouldReturnEmptyPageWhenNoMessagesMatch() {
        var pageable = PageRequest.of(0, 20, Sort.by(Sort.Direction.DESC, "timestamp", "id"));
        when(clientRepository.existsById(CLIENT_ID)).thenReturn(true);
        when(messageRepository.searchOwnedMessages(CLIENT_ID, null, null, null, null, pageable))
                .thenReturn(Page.empty(pageable));
        var result = service.listMessages(CLIENT_ID, null, null, null, null, 0, 20);
        assertThat(result.content()).isEmpty();
        assertThat(result.totalElements()).isZero();
        assertThat(result.totalPages()).isZero();
    }

    @Test
    void shouldKeepTotalsOnPageBeyondLastMessage() {
        var pageable = PageRequest.of(5, 20, Sort.by(Sort.Direction.DESC, "timestamp", "id"));
        when(clientRepository.existsById(CLIENT_ID)).thenReturn(true);
        when(messageRepository.searchOwnedMessages(CLIENT_ID, null, null, null, null, pageable))
                .thenReturn(new PageImpl<>(List.of(), pageable, 3));
        var result = service.listMessages(CLIENT_ID, null, null, null, null, 5, 20);
        assertThat(result.content()).isEmpty();
        assertThat(result.page()).isEqualTo(5);
        assertThat(result.totalElements()).isEqualTo(3);
        assertThat(result.totalPages()).isEqualTo(1);
    }

    @ParameterizedTest
    @EnumSource(value = Operation.class, names = {"LIST", "HISTORY"})
    void shouldRejectMissingOrUnownedConversation(Operation operation) {
        when(clientRepository.existsById(CLIENT_ID)).thenReturn(true);
        when(conversationRepository.existsByIdAndClientId(CONVERSATION_ID, CLIENT_ID)).thenReturn(false);
        assertThatThrownBy(() -> invoke(operation, CLIENT_ID))
                .isInstanceOf(ConversationNotFoundException.class);
        verifyNoInteractions(messageRepository);
    }

    @Test
    void shouldRequestHistoryInChronologicalOrder() {
        var pageable = PageRequest.of(0, 20, Sort.by(Sort.Direction.ASC, "timestamp", "id"));
        when(clientRepository.existsById(CLIENT_ID)).thenReturn(true);
        when(conversationRepository.existsByIdAndClientId(CONVERSATION_ID, CLIENT_ID)).thenReturn(true);
        when(messageRepository.searchOwnedMessages(CLIENT_ID, CONVERSATION_ID, null, null, null, pageable))
                .thenReturn(new PageImpl<>(List.of(message()), pageable, 1));
        var result = service.getHistory(CLIENT_ID, CONVERSATION_ID, 0, 20);
        assertThat(result.content()).hasSize(1);
        assertThat(result.content().get(0).conversationId()).isEqualTo(CONVERSATION_ID);
        verify(messageRepository).searchOwnedMessages(CLIENT_ID, CONVERSATION_ID, null, null, null, pageable);
    }

    @ParameterizedTest
    @CsvSource({"-1,20", "0,0", "0,-1", "0,101"})
    void shouldRejectInvalidPaginationForListAndHistory(int page, int size) {
        assertThatThrownBy(() -> service.listMessages(CLIENT_ID, null, null, null, null, page, size))
                .isInstanceOf(InvalidInputException.class);
        assertThatThrownBy(() -> service.getHistory(CLIENT_ID, CONVERSATION_ID, page, size))
                .isInstanceOf(InvalidInputException.class);
        verifyNoInteractions(clientRepository, conversationRepository, messageRepository);
    }

    private void invoke(Operation operation, Long clientId) {
        switch (operation) {
            case DETAIL -> service.getMessage(clientId, MESSAGE_ID);
            case STATUS -> service.getStatus(clientId, MESSAGE_ID);
            case LIST -> service.listMessages(clientId, CONVERSATION_ID, null, null, null, 0, 20);
            case HISTORY -> service.getHistory(clientId, CONVERSATION_ID, 0, 20);
        }
    }

    private Message message() {
        Client client = new Client("Cliente", "52998224725", DocumentType.CPF, PlanType.PREPAID);
        ReflectionTestUtils.setField(client, "id", CLIENT_ID);
        Recipient recipient = new Recipient("Maria", "+5544999990000");
        ReflectionTestUtils.setField(recipient, "id", 7L);
        Conversation conversation = new Conversation(client, recipient);
        ReflectionTestUtils.setField(conversation, "id", CONVERSATION_ID);
        Message message = new Message(conversation, client, "Olá", TIME,
                PriorityType.NORMAL, ChannelType.WHATSAPP);
        ReflectionTestUtils.setField(message, "id", MESSAGE_ID);
        message.startProcessing();
        message.markAsSent();
        return message;
    }
}