package com.irrah.desafio_tecnico.message;

import com.irrah.desafio_tecnico.client.ClientIdentificationService;
import com.irrah.desafio_tecnico.message.dto.MessageResponse;
import com.irrah.desafio_tecnico.message.dto.MessageStatusResponse;
import com.irrah.desafio_tecnico.message.dto.NewMessageRequest;
import com.irrah.desafio_tecnico.message.dto.NewMessageResponse;
import com.irrah.desafio_tecnico.shared.dto.PageResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RequiredArgsConstructor
@RestController
@RequestMapping("/messages")
public class MessageController {
    private final MessageService messageService;
    private final MessageQueryService messageQueryService;
    private final ClientIdentificationService identificationService;

    @PostMapping
    public ResponseEntity<NewMessageResponse> newMessage(
            @RequestHeader("X-Client-Document") String document,
            @Valid @RequestBody NewMessageRequest request
    ) {
        Long clientId = identificationService.identify(document);

        return ResponseEntity.status(201).body(messageService.newMessage(clientId, request));
    }

    @GetMapping
    public ResponseEntity<PageResponse<MessageResponse>> listMessages(
            @RequestHeader("X-Client-Document") String document,
            @RequestParam(name = "conversationId", required = false) Long conversationId,
            @RequestParam(name = "status", required = false) StatusType status,
            @RequestParam(name = "priority", required = false) PriorityType priority,
            @RequestParam(name = "channel", required = false) ChannelType channel,
            @RequestParam(name = "page", defaultValue = "0") int page,
            @RequestParam(name = "size", defaultValue = "20") int size
    ) {
        Long clientId = identificationService.identify(document);

        return ResponseEntity.ok(messageQueryService.listMessages(
                clientId, conversationId, status, priority, channel, page, size));
    }

    @GetMapping("/{id}")
    public ResponseEntity<MessageResponse> getMessage(
            @PathVariable("id") Long messageId,
            @RequestHeader("X-Client-Document") String document
    ) {
        Long clientId = identificationService.identify(document);

        return ResponseEntity.ok(messageQueryService.getMessage(clientId, messageId));
    }

    @GetMapping("/{id}/status")
    public ResponseEntity<MessageStatusResponse> getStatus(
            @PathVariable("id") Long messageId,
            @RequestHeader("X-Client-Document") String document
    ) {
        Long clientId = identificationService.identify(document);

        return ResponseEntity.ok(messageQueryService.getStatus(clientId, messageId));
    }

    @PostMapping("/{id}/delivery-confirmation")
    public ResponseEntity<MessageStatusResponse> deliveryConfirm(            @PathVariable("id") Long messageId,
                                                                             @RequestHeader("X-Client-Document") String document
    ){
        Long clientId = identificationService.identify(document);

        return ResponseEntity.ok(messageService.confirmDelivery(clientId, messageId));
    }

    @PostMapping("/{id}/read-confirmation")
    public ResponseEntity<MessageStatusResponse> readConfirm(                @PathVariable("id") Long messageId,
                                                                             @RequestHeader("X-Client-Document") String document
    ){
        Long clientId = identificationService.identify(document);

        return ResponseEntity.ok(messageService.confirmRead(clientId, messageId));
    }
}