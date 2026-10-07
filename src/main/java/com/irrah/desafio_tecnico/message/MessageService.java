package com.irrah.desafio_tecnico.message;

import com.irrah.desafio_tecnico.message.dto.MessageStatusResponse;
import com.irrah.desafio_tecnico.message.dto.NewMessageRequest;
import com.irrah.desafio_tecnico.message.dto.NewMessageResponse;
import com.irrah.desafio_tecnico.message.exception.MessageNotFoundException;
import com.irrah.desafio_tecnico.queue.InMemoryMessageQueue;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;


@RequiredArgsConstructor
@Service
public class MessageService {
    private final InMemoryMessageQueue inMemoryMessageQueue;
    private final MessageRegistrationService messageRegistrationService;
    private final MessageRepository messageRepository;

    public NewMessageResponse newMessage(Long id, NewMessageRequest request){
        Message message =  messageRegistrationService.register(id, request);

        NewMessageResponse response = NewMessageResponse.builder()
                .messageId(message.getId())
                .statusType(message.getStatus())
                .build();

        inMemoryMessageQueue.enqueue(message.getId(), message.getPriority());

        return response;
    }

    @Transactional
    public MessageStatusResponse confirmDelivery(
            Long clientId,
            Long messageId
    ) {
        Message message = messageRepository
                .findOwnedMessage(messageId, clientId)
                .orElseThrow(MessageNotFoundException::new);

        message.markAsDelivered();

        return MessageStatusResponse.builder()
                .messageId(message.getId())
                .status(message.getStatus())
                .build();
    }

    @Transactional
    public MessageStatusResponse confirmRead(
            Long clientId,
            Long messageId
    ) {
        Message message = messageRepository
                .findOwnedMessage(messageId, clientId)
                .orElseThrow(MessageNotFoundException::new);

        message.markAsRead();

        return MessageStatusResponse.builder()
                .messageId(message.getId())
                .status(message.getStatus())
                .build();
    }
}
