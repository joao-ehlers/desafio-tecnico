package com.irrah.desafio_tecnico.conversation;

import com.irrah.desafio_tecnico.client.ClientRepository;
import com.irrah.desafio_tecnico.client.exception.ClientNotFoundException;
import com.irrah.desafio_tecnico.conversation.dto.ConversationResponse;
import com.irrah.desafio_tecnico.conversation.exception.ConversationNotFoundException;
import com.irrah.desafio_tecnico.message.MessageRepository;
import com.irrah.desafio_tecnico.message.StatusType;
import com.irrah.desafio_tecnico.message.projection.LatestMessageView;
import com.irrah.desafio_tecnico.message.projection.UnreadCountView;
import com.irrah.desafio_tecnico.shared.dto.PageResponse;
import com.irrah.desafio_tecnico.shared.query.QueryPage;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@RequiredArgsConstructor
@Service
@Transactional(readOnly = true)
public class ConversationQueryService {
    private final ConversationRepository conversationRepository;
    private final MessageRepository messageRepository;
    private final ClientRepository clientRepository;

    public PageResponse<ConversationResponse> listConversations(
            Long clientId, int page, int size
    ) {
        var pageable = QueryPage.of(page, size, Sort.by(Sort.Direction.DESC, "id"));
        requireClient(clientId);
        var conversations = conversationRepository.findByClientId(clientId, pageable);
        if (!conversations.hasContent()) {
            return new PageResponse<>(List.of(), page, size,
                    conversations.getTotalElements(), conversations.getTotalPages());
        }
        var ids = conversations.getContent().stream().map(Conversation::getId).toList();
        SummaryData summaries = loadSummaries(clientId, ids);
        return PageResponse.from(conversations.map(c -> toResponse(c, summaries)));
    }

    public ConversationResponse getConversation(Long clientId, Long conversationId) {
        requireClient(clientId);
        Conversation conversation = conversationRepository
                .findByIdAndClientId(conversationId, clientId)
                .orElseThrow(ConversationNotFoundException::new);
        return toResponse(conversation, loadSummaries(clientId, List.of(conversationId)));
    }

    private SummaryData loadSummaries(Long clientId, List<Long> ids) {
        Map<Long, LatestMessageView> latest = messageRepository
                .findLatestMessages(clientId, ids).stream()
                .collect(Collectors.toMap(LatestMessageView::getConversationId,
                        Function.identity()));
        Map<Long, Long> unread = messageRepository.countUnreadMessages(
                clientId, ids, List.of(StatusType.SENT, StatusType.DELIVERED)
        ).stream().collect(Collectors.toMap(UnreadCountView::getConversationId,
                UnreadCountView::getUnreadCount));
        return new SummaryData(latest, unread);
    }

    private ConversationResponse toResponse(Conversation c, SummaryData summaries) {
        LatestMessageView latest = summaries.latest().get(c.getId());
        return new ConversationResponse(
                c.getId(), c.getClient().getId(), c.getRecipient().getId(),
                c.getRecipient().getName(),
                latest == null ? null : latest.getContent(),
                latest == null ? null : latest.getTimestamp(),
                summaries.unread().getOrDefault(c.getId(), 0L));
    }

    private void requireClient(Long clientId) {
        if (clientId == null || !clientRepository.existsById(clientId)) {
            throw new ClientNotFoundException();
        }
    }

    private record SummaryData(
            Map<Long, LatestMessageView> latest, Map<Long, Long> unread
    ) {}
}