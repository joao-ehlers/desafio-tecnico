package com.irrah.desafio_tecnico.message;

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

    @PostMapping
    public ResponseEntity<NewMessageResponse> newMessage(
            @Valid @RequestBody NewMessageRequest request
    ) {
        return ResponseEntity.status(201).body(messageService.newMessage(request));
    }

    @GetMapping
    public ResponseEntity<PageResponse<MessageResponse>> listMessages(
            @RequestParam("clientId") Long clientId,
            @RequestParam(name = "conversationId", required = false) Long conversationId,
            @RequestParam(name = "status", required = false) StatusType status,
            @RequestParam(name = "priority", required = false) PriorityType priority,
            @RequestParam(name = "channel", required = false) ChannelType channel,
            @RequestParam(name = "page", defaultValue = "0") int page,
            @RequestParam(name = "size", defaultValue = "20") int size
    ) {
        return ResponseEntity.ok(messageQueryService.listMessages(
                clientId, conversationId, status, priority, channel, page, size));
    }

    @GetMapping("/{id}")
    public ResponseEntity<MessageResponse> getMessage(
            @PathVariable("id") Long messageId,
            @RequestParam("clientId") Long clientId
    ) {
        return ResponseEntity.ok(messageQueryService.getMessage(clientId, messageId));
    }

    @GetMapping("/{id}/status")
    public ResponseEntity<MessageStatusResponse> getStatus(
            @PathVariable("id") Long messageId,
            @RequestParam("clientId") Long clientId
    ) {
        return ResponseEntity.ok(messageQueryService.getStatus(clientId, messageId));
    }
}