package com.irrah.desafio_tecnico.message;

import com.irrah.desafio_tecnico.client.ClientIdentificationService;
import com.irrah.desafio_tecnico.message.dto.MessageResponse;
import com.irrah.desafio_tecnico.message.dto.MessageStatusResponse;
import com.irrah.desafio_tecnico.message.dto.NewMessageRequest;
import com.irrah.desafio_tecnico.message.dto.NewMessageResponse;
import com.irrah.desafio_tecnico.shared.dto.PageResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Tag(name = "Mensagens", description = "Envio, consulta e confirmações simuladas")
@RequiredArgsConstructor
@RestController
@RequestMapping("/messages")
public class MessageController {
    private final MessageService messageService;
    private final MessageQueryService messageQueryService;
    private final ClientIdentificationService identificationService;

    @Operation(
            summary = "Registrar e enfileirar uma mensagem",
            description = """
                Identifica o cliente pelo documento, registra a mensagem e cobra
                uma única vez. Retorna o estado inicial QUEUED sem aguardar o envio.
                O processamento ocorre em background. Consulte o endpoint de status
                para acompanhar o resultado. Entrega real não é realizada.
                """
    )
    @ApiResponse(
            responseCode = "201",
            description = "Mensagem registrada e enfileirada",
            content = @Content(
                    mediaType = "application/json",
                    schema = @Schema(implementation = NewMessageResponse.class)
            )
    )
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

    @Operation(
            summary = "Consultar status da mensagem",
            description = "FAILED com retry pendente é diferente de falha definitiva. O retorno traz apenas o status."
    )
    @GetMapping("/{id}/status")
    public ResponseEntity<MessageStatusResponse> getStatus(
            @PathVariable("id") Long messageId,
            @RequestHeader("X-Client-Document") String document
    ) {
        Long clientId = identificationService.identify(document);

        return ResponseEntity.ok(messageQueryService.getStatus(clientId, messageId));
    }

    @Operation(
            summary = "Confirmar entrega (simulado)",
            description = "Evento simulado. Exige que a mensagem esteja em SENT. Não enfileira nem cobra novamente."
    )
    @PostMapping("/{id}/delivery-confirmation")
    public ResponseEntity<MessageStatusResponse> deliveryConfirm(            @PathVariable("id") Long messageId,
                                                                             @RequestHeader("X-Client-Document") String document
    ){
        Long clientId = identificationService.identify(document);

        return ResponseEntity.ok(messageService.confirmDelivery(clientId, messageId));
    }

    @Operation(
            summary = "Confirmar leitura (simulado)",
            description = "Evento simulado. Exige que a mensagem esteja em DELIVERED. Não é comprovação real de leitura."
    )
    @PostMapping("/{id}/read-confirmation")
    public ResponseEntity<MessageStatusResponse> readConfirm(                @PathVariable("id") Long messageId,
                                                                             @RequestHeader("X-Client-Document") String document
    ){
        Long clientId = identificationService.identify(document);

        return ResponseEntity.ok(messageService.confirmRead(clientId, messageId));
    }
}