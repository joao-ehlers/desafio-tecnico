package com.irrah.desafio_tecnico.message;

import com.irrah.desafio_tecnico.client.ClientRepository;
import com.irrah.desafio_tecnico.client.exception.ClientNotFoundException;
import com.irrah.desafio_tecnico.conversation.ConversationRepository;
import com.irrah.desafio_tecnico.conversation.exception.ConversationNotFoundException;
import com.irrah.desafio_tecnico.message.dto.MessageResponse;
import com.irrah.desafio_tecnico.message.dto.MessageStatusResponse;
import com.irrah.desafio_tecnico.message.exception.MessageNotFoundException;
import com.irrah.desafio_tecnico.shared.dto.PageResponse;
import com.irrah.desafio_tecnico.shared.query.QueryPage;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@RequiredArgsConstructor
@Service
@Transactional(readOnly = true)
public class MessageQueryService {
    private final MessageRepository messageRepository;
    private final ConversationRepository conversationRepository;
    private final ClientRepository clientRepository;

    public MessageResponse getMessage(Long clientId, Long messageId) {
        requireClient(clientId);
        return MessageResponse.from(findOwnedMessage(clientId, messageId));
    }

    public MessageStatusResponse getStatus(Long clientId, Long messageId) {
        requireClient(clientId);
        Message message = findOwnedMessage(clientId, messageId);
        return new MessageStatusResponse(message.getId(), message.getStatus());
    }

    public PageResponse<MessageResponse> listMessages(
            Long clientId, Long conversationId, StatusType status,
            PriorityType priority, ChannelType channel, int page, int size
    ) {
        var pageable = QueryPage.of(page, size,
                Sort.by(Sort.Direction.DESC, "timestamp", "id"));
        requireClient(clientId);
        if (conversationId != null) {
            requireConversation(clientId, conversationId);
        }
        return PageResponse.from(messageRepository.searchOwnedMessages(
                clientId, conversationId, status, priority, channel, pageable
        ).map(MessageResponse::from));
    }

    public PageResponse<MessageResponse> getHistory(
            Long clientId, Long conversationId, int page, int size
    ) {
        var pageable = QueryPage.of(page, size,
                Sort.by(Sort.Direction.ASC, "timestamp", "id"));
        requireClient(clientId);
        requireConversation(clientId, conversationId);
        return PageResponse.from(messageRepository.searchOwnedMessages(
                clientId, conversationId, null, null, null, pageable
        ).map(MessageResponse::from));
    }

    private Message findOwnedMessage(Long clientId, Long messageId) {
        return messageRepository.findOwnedMessage(messageId, clientId)
                .orElseThrow(MessageNotFoundException::new);
    }

    private void requireClient(Long clientId) {
        if (clientId == null || !clientRepository.existsById(clientId)) {
            throw new ClientNotFoundException();
        }
    }

    private void requireConversation(Long clientId, Long conversationId) {
        if (!conversationRepository.existsByIdAndClientId(conversationId, clientId)) {
            throw new ConversationNotFoundException();
        }
    }
}