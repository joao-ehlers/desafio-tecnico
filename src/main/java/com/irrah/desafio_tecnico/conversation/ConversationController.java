package com.irrah.desafio_tecnico.conversation;

import com.irrah.desafio_tecnico.client.ClientIdentificationService;
import com.irrah.desafio_tecnico.conversation.dto.ConversationResponse;
import com.irrah.desafio_tecnico.message.MessageQueryService;
import com.irrah.desafio_tecnico.message.dto.MessageResponse;
import com.irrah.desafio_tecnico.shared.dto.PageResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Tag(name = "Conversas", description = "Listagem de conversas e histórico de mensagens do cliente")
@RequiredArgsConstructor
@RestController
@RequestMapping("/conversations")
public class ConversationController {
    private final ConversationQueryService conversationQueryService;
    private final MessageQueryService messageQueryService;
    private final ClientIdentificationService identificationService;

    @GetMapping
    public ResponseEntity<PageResponse<ConversationResponse>> listConversations(
            @RequestHeader("X-Client-Document") String document,
            @RequestParam(name = "page", defaultValue = "0") int page,
            @RequestParam(name = "size", defaultValue = "20") int size
    ) {
        Long clientId = identificationService.identify(document);

        return ResponseEntity.ok(conversationQueryService.listConversations(clientId, page, size));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ConversationResponse> getConversation(
            @PathVariable("id") Long conversationId,
            @RequestHeader("X-Client-Document") String document
    ) {
        Long clientId = identificationService.identify(document);

        return ResponseEntity.ok(conversationQueryService.getConversation(clientId, conversationId));
    }

    @GetMapping("/{id}/messages")
    public ResponseEntity<PageResponse<MessageResponse>> getHistory(
            @PathVariable("id") Long conversationId,
            @RequestHeader("X-Client-Document") String document,
            @RequestParam(name = "page", defaultValue = "0") int page,
            @RequestParam(name = "size", defaultValue = "20") int size
    ) {
        Long clientId = identificationService.identify(document);

        return ResponseEntity.ok(messageQueryService.getHistory(clientId, conversationId, page, size));
    }
}