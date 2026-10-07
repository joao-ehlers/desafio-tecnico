package com.irrah.desafio_tecnico.message;

import com.irrah.desafio_tecnico.billing.BillingService;
import com.irrah.desafio_tecnico.client.Client;
import com.irrah.desafio_tecnico.client.ClientRepository;
import com.irrah.desafio_tecnico.client.exception.ClientNotFoundException;
import com.irrah.desafio_tecnico.client.exception.InactiveClientException;
import com.irrah.desafio_tecnico.conversation.Conversation;
import com.irrah.desafio_tecnico.conversation.ConversationRepository;
import com.irrah.desafio_tecnico.conversation.Recipient;
import com.irrah.desafio_tecnico.conversation.RecipientRepository;
import com.irrah.desafio_tecnico.conversation.exception.ConversationNotFoundException;
import com.irrah.desafio_tecnico.conversation.exception.RecipientNotFoundException;
import com.irrah.desafio_tecnico.message.dto.NewMessageRequest;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.Instant;

@RequiredArgsConstructor
@Service
public class MessageRegistrationService {

    private final MessageRepository messageRepository;
    private final ConversationRepository conversationRepository;
    private final RecipientRepository recipientRepository;
    private final ClientRepository clientRepository;
    private final BillingService billingService;
    private final Clock clock;

    @Transactional
    public Message register(Long id, NewMessageRequest request){

        Client client = clientRepository.findById(id).orElseThrow(ClientNotFoundException::new);


        if (!client.isActive()) {
            throw new InactiveClientException();
        }

        Conversation conversation = resolveConversation(request, client);

        Message message = new Message(conversation, client, request.content(), Instant.now(clock),
                request.priorityType(), request.channelType());

        messageRepository.save(message);

        billingService.chargeMessage(client, message);

        return message;
    }

    private Conversation resolveConversation(NewMessageRequest request, Client client){
        if(request.conversationId() != null){
            Conversation conversation = conversationRepository.findByIdAndClientId(request.conversationId(),
                    client.getId()).orElseThrow(ConversationNotFoundException::new);

            if(request.recipientId() != null && !request.recipientId().equals(conversation.getRecipient().getId())){
                throw new IllegalArgumentException("O destinatario nao pertence a essa conversa");
            }

            return conversation;
        }

        Recipient recipient = resolveRecipient(request);

        return conversationRepository.save(new Conversation(client, recipient));
    }

    private Recipient resolveRecipient(NewMessageRequest request){
        if(request.recipientId() != null){
            return recipientRepository.findById(request.recipientId()).orElseThrow(RecipientNotFoundException::new);
        }

        return recipientRepository.save(new Recipient(request.recipientName(), request.recipientPhone()));
    }

}
