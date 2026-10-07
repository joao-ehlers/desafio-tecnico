package com.irrah.desafio_tecnico.conversation;

import com.irrah.desafio_tecnico.client.*;
import com.irrah.desafio_tecnico.client.exception.ClientNotFoundException;
import com.irrah.desafio_tecnico.conversation.exception.ConversationNotFoundException;
import com.irrah.desafio_tecnico.message.MessageRepository;
import com.irrah.desafio_tecnico.message.StatusType;
import com.irrah.desafio_tecnico.message.projection.LatestMessageView;
import com.irrah.desafio_tecnico.message.projection.UnreadCountView;
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
class ConversationQueryServiceTest {
    private static final Long CLIENT_ID = 1L;
    private static final Instant TIME = Instant.parse("2026-10-06T00:00:00Z");
    private static final List<StatusType> UNREAD = List.of(StatusType.SENT, StatusType.DELIVERED);
    @Mock private ConversationRepository conversationRepository;
    @Mock private MessageRepository messageRepository;
    @Mock private ClientRepository clientRepository;
    private ConversationQueryService service;

    @BeforeEach
    void setUp() {
        service = new ConversationQueryService(conversationRepository, messageRepository, clientRepository);
    }

    @Test
    void shouldMapConversationAndItsSummary() {

        var conversation = conversation(10L, 7L, "Maria");
        var latestMessage = latest(10L, "Olá", TIME);
        var unreadCount = unread(10L, 2L);

        when(clientRepository.existsById(CLIENT_ID))
                .thenReturn(true);

        when(conversationRepository.findByIdAndClientId(10L, CLIENT_ID))
                .thenReturn(Optional.of(conversation));

        when(messageRepository.findLatestMessages(CLIENT_ID, List.of(10L)))
                .thenReturn(List.of(latestMessage));

        when(messageRepository.countUnreadMessages(
                CLIENT_ID, List.of(10L), UNREAD))
                .thenReturn(List.of(unreadCount));

        var result = service.getConversation(CLIENT_ID, 10L);

        assertThat(result.id()).isEqualTo(10L);
        assertThat(result.clientId()).isEqualTo(CLIENT_ID);
        assertThat(result.recipientId()).isEqualTo(7L);
        assertThat(result.recipientName()).isEqualTo("Maria");
        assertThat(result.lastMessageContent()).isEqualTo("Olá");
        assertThat(result.lastMessageTime()).isEqualTo(TIME);
        assertThat(result.unreadCount()).isEqualTo(2);
        verify(messageRepository).countUnreadMessages(CLIENT_ID, List.of(10L), UNREAD);
    }

    @Test
    void shouldAssociateBatchedSummariesByConversationIdNotResultPosition() {
        var pageable = PageRequest.of(
                0, 20, Sort.by(Sort.Direction.DESC, "id")
        );
        var ids = List.of(20L, 10L);

        var anaConversation = conversation(20L, 8L, "Ana");
        var mariaConversation = conversation(10L, 7L, "Maria");

        var mariaLatest = latest(10L, "Mensagem de Maria", TIME);
        var anaLatest = latest(
                20L, "Mensagem de Ana", TIME.plusSeconds(1)
        );
        var mariaUnread = unread(10L, 3L);

        when(clientRepository.existsById(CLIENT_ID))
                .thenReturn(true);

        when(conversationRepository.findByClientId(CLIENT_ID, pageable))
                .thenReturn(new PageImpl<>(
                        List.of(anaConversation, mariaConversation),
                        pageable,
                        2
                ));

        when(messageRepository.findLatestMessages(CLIENT_ID, ids))
                .thenReturn(List.of(mariaLatest, anaLatest));

        when(messageRepository.countUnreadMessages(CLIENT_ID, ids, UNREAD))
                .thenReturn(List.of(mariaUnread));

        var result = service.listConversations(CLIENT_ID, 0, 20);

        assertThat(result.content()).hasSize(2);
        var first = result.content().get(0);
        var second = result.content().get(1);
        assertThat(first.id()).isEqualTo(20L);
        assertThat(first.lastMessageContent()).isEqualTo("Mensagem de Ana");
        assertThat(first.lastMessageTime()).isEqualTo(TIME.plusSeconds(1));
        assertThat(first.unreadCount()).isZero();
        assertThat(second.id()).isEqualTo(10L);
        assertThat(second.lastMessageContent()).isEqualTo("Mensagem de Maria");
        assertThat(second.unreadCount()).isEqualTo(3);
        assertThat(result.page()).isZero();
        assertThat(result.size()).isEqualTo(20);
        assertThat(result.totalElements()).isEqualTo(2);
        assertThat(result.totalPages()).isEqualTo(1);
        verify(messageRepository).findLatestMessages(CLIENT_ID, ids);
        verify(messageRepository).countUnreadMessages(CLIENT_ID, ids, UNREAD);
        verifyNoMoreInteractions(messageRepository);
    }

    @Test
    void shouldReturnNullSummaryAndZeroCountForConversationWithoutMessages() {
        when(clientRepository.existsById(CLIENT_ID)).thenReturn(true);
        when(conversationRepository.findByIdAndClientId(10L, CLIENT_ID))
                .thenReturn(Optional.of(conversation(10L, 7L, "Maria")));
        when(messageRepository.findLatestMessages(CLIENT_ID, List.of(10L))).thenReturn(List.of());
        when(messageRepository.countUnreadMessages(CLIENT_ID, List.of(10L), UNREAD)).thenReturn(List.of());
        var result = service.getConversation(CLIENT_ID, 10L);
        assertThat(result.lastMessageContent()).isNull();
        assertThat(result.lastMessageTime()).isNull();
        assertThat(result.unreadCount()).isZero();
    }

    @Test
    void shouldNotFetchSummariesForEmptyPage() {
        var pageable = PageRequest.of(0, 20, Sort.by(Sort.Direction.DESC, "id"));
        when(clientRepository.existsById(CLIENT_ID)).thenReturn(true);
        when(conversationRepository.findByClientId(CLIENT_ID, pageable)).thenReturn(Page.empty(pageable));
        var result = service.listConversations(CLIENT_ID, 0, 20);
        assertThat(result.content()).isEmpty();
        assertThat(result.totalElements()).isZero();
        assertThat(result.totalPages()).isZero();
        verifyNoInteractions(messageRepository);
    }

    @Test
    void shouldKeepTotalsWhenRequestedPageIsBeyondTheEnd() {
        var pageable = PageRequest.of(5, 20, Sort.by(Sort.Direction.DESC, "id"));
        when(clientRepository.existsById(CLIENT_ID)).thenReturn(true);
        when(conversationRepository.findByClientId(CLIENT_ID, pageable))
                .thenReturn(new PageImpl<>(List.of(), pageable, 3));
        var result = service.listConversations(CLIENT_ID, 5, 20);
        assertThat(result.content()).isEmpty();
        assertThat(result.page()).isEqualTo(5);
        assertThat(result.size()).isEqualTo(20);
        assertThat(result.totalElements()).isEqualTo(3);
        assertThat(result.totalPages()).isEqualTo(1);
        verifyNoInteractions(messageRepository);
    }

    @Test
    void shouldRejectMissingOrUnownedConversationWithoutFetchingSummaries() {
        when(clientRepository.existsById(CLIENT_ID)).thenReturn(true);
        when(conversationRepository.findByIdAndClientId(10L, CLIENT_ID)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.getConversation(CLIENT_ID, 10L))
                .isInstanceOf(ConversationNotFoundException.class);
        verify(conversationRepository).findByIdAndClientId(10L, CLIENT_ID);
        verify(conversationRepository, never()).findById(anyLong());
        verifyNoInteractions(messageRepository);
    }

    enum Operation { DETAIL, LIST }

    @ParameterizedTest
    @EnumSource(Operation.class)
    void shouldRejectUnknownClient(Operation operation) {
        when(clientRepository.existsById(CLIENT_ID)).thenReturn(false);
        assertThatThrownBy(() -> invoke(operation, CLIENT_ID))
                .isInstanceOf(ClientNotFoundException.class);
        verifyNoInteractions(conversationRepository, messageRepository);
    }

    @ParameterizedTest
    @EnumSource(Operation.class)
    void shouldRejectNullClientWithoutQueryingRepositories(Operation operation) {
        assertThatThrownBy(() -> invoke(operation, null)).isInstanceOf(ClientNotFoundException.class);
        verifyNoInteractions(clientRepository, conversationRepository, messageRepository);
    }

    @ParameterizedTest
    @CsvSource({"-1,20", "0,0", "0,-1", "0,101"})
    void shouldRejectInvalidPagination(int page, int size) {
        assertThatThrownBy(() -> service.listConversations(CLIENT_ID, page, size))
                .isInstanceOf(InvalidInputException.class);
        verifyNoInteractions(clientRepository, conversationRepository, messageRepository);
    }

    @ParameterizedTest
    @CsvSource({"0,1", "0,100"})
    void shouldAcceptPaginationSizeBoundaries(int page, int size) {
        var pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "id"));
        when(clientRepository.existsById(CLIENT_ID)).thenReturn(true);
        when(conversationRepository.findByClientId(CLIENT_ID, pageable)).thenReturn(Page.empty(pageable));
        assertThat(service.listConversations(CLIENT_ID, page, size).size()).isEqualTo(size);
    }

    private void invoke(Operation operation, Long clientId) {
        switch (operation) {
            case DETAIL -> service.getConversation(clientId, 10L);
            case LIST -> service.listConversations(clientId, 0, 20);
        }
    }

    private Conversation conversation(Long id, Long recipientId, String name) {
        Client client = new Client("Cliente", "52998224725", DocumentType.CPF, PlanType.PREPAID);
        ReflectionTestUtils.setField(client, "id", CLIENT_ID);
        Recipient recipient = new Recipient(name, "+5544999990000");
        ReflectionTestUtils.setField(recipient, "id", recipientId);
        Conversation conversation = new Conversation(client, recipient);
        ReflectionTestUtils.setField(conversation, "id", id);
        return conversation;
    }

    private LatestMessageView latest(Long id, String content, Instant timestamp) {
        LatestMessageView result = mock(LatestMessageView.class);
        when(result.getConversationId()).thenReturn(id);
        when(result.getContent()).thenReturn(content);
        when(result.getTimestamp()).thenReturn(timestamp);
        return result;
    }

    private UnreadCountView unread(Long id, Long count) {
        UnreadCountView result = mock(UnreadCountView.class);
        when(result.getConversationId()).thenReturn(id);
        when(result.getUnreadCount()).thenReturn(count);
        return result;
    }
}