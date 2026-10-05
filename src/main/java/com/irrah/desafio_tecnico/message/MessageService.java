package com.irrah.desafio_tecnico.message;

import com.irrah.desafio_tecnico.client.Client;
import com.irrah.desafio_tecnico.client.ClientRepository;
import com.irrah.desafio_tecnico.client.exception.ClientNotFoundException;
import com.irrah.desafio_tecnico.conversation.Conversation;
import com.irrah.desafio_tecnico.conversation.ConversationRepository;
import com.irrah.desafio_tecnico.conversation.Recipient;
import com.irrah.desafio_tecnico.conversation.RecipientRepository;
import com.irrah.desafio_tecnico.message.dto.NewMessageRequest;
import com.irrah.desafio_tecnico.message.dto.NewMessageResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@RequiredArgsConstructor
@Service
public class MessageService {

    private final MessageRepository messageRepository;
    private final ConversationRepository conversationRepository;
    private final RecipientRepository recipientRepository;
    private final ClientRepository clientRepository;

    public NewMessageResponse newMessage(NewMessageRequest request){
        Client client = clientRepository.findById(request.clientId()).orElseThrow(ClientNotFoundException::new);
        Recipient recipient = recipientRepository.findById(request.recipientId())
                .orElse(new Recipient(request.recipientName(), request.recipientPhone()));
        Conversation conversation = conversationRepository.findById(request.conversationId())
                .orElse(new Conversation(client, recipient));

        Message message = new Message(conversation, client, request.content(), request.timestamp(),
                request.priorityType(), request.channelType());


    }

}
