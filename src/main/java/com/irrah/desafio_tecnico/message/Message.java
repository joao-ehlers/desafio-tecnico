package com.irrah.desafio_tecnico.message;

import com.irrah.desafio_tecnico.client.Client;
import com.irrah.desafio_tecnico.conversation.Conversation;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.math.BigDecimal;
import java.time.Instant;


@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Getter
@Entity
@Table(name = "messages")
public class Message {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "conversation_id", nullable = false)
    private Conversation conversation;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sender_id", nullable = false)
    private Client sender;

    @NotBlank(message = "o conteudo da mensagem é obrigatorio")
    @Column(nullable = false, length = 2000)
    private String content;

    @NotNull
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant timestamp;

    @NotNull(message = "a prioridade da mensagem é obrigatoria")
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PriorityType priority;

    @NotNull(message = "o status da mensagem é obrigatorio")
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private StatusType status = StatusType.QUEUED;

    @NotNull(message = "o custo da mensagem é obrigatorio")
    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal cost;

    @NotNull(message = "o canal da mensagem é obrigatorio")
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ChannelType channel;

    public Message(Conversation conversation, Client sender, String content, Instant timestamp, PriorityType priority, ChannelType channel) {

        if (conversation == null) {
            throw new IllegalArgumentException("a conversa é obrigatória");
        }

        if (sender == null) {
            throw new IllegalArgumentException("o remetente é obrigatório");
        }

        if (content == null || content.isBlank()) {
            throw new IllegalArgumentException("o conteúdo é obrigatório");
        }

        if(content.length() > 2000){
            throw new IllegalArgumentException("o conteudo pode ter no maximo 2000 caracteres");
        }

        if (timestamp == null) {
            throw new IllegalArgumentException("a data de criação é obrigatória");
        }

        if (priority == null) {
            throw new IllegalArgumentException("a prioridade é obrigatória");
        }

        if (channel == null){
            throw new IllegalArgumentException("o tipo de canal é obrigatório");
        }

        this.conversation = conversation;
        this.sender = sender;
        this.content = content;
        this.timestamp = timestamp;
        this.priority = priority;
        this.cost = switch(priority){
            case NORMAL -> new BigDecimal("0.25");
            case URGENT -> new BigDecimal("0.50");
        };
        this.channel=channel;
    }

    public void startProcessing(){
        if(this.status != StatusType.QUEUED){
            throw new IllegalStateException("Somente mensagens enfileiradas podem iniciar o processamento");
        }

        this.status = StatusType.PROCESSING;
    }

    public void markAsSent(){
        if(this.status != StatusType.PROCESSING){
            throw new IllegalStateException("Somente mensagens em processamento podem ser enviadas");
        }

        this.status = StatusType.SENT;
    }

    public void markAsDelivered(){
        if(this.status != StatusType.SENT){
            throw new IllegalStateException("Somente mensagens enviadas podem ser entregues");
        }

        this.status = StatusType.DELIVERED;
    }

    public void markAsFailed(){
        if(this.status != StatusType.PROCESSING){
            throw new IllegalStateException("Somente mensagens em processamento podem  falhar");
        }

        this.status = StatusType.FAILED;
    }

    public void markAsRead(){
        if(this.status != StatusType.DELIVERED){
            throw new IllegalStateException("Somente mensagens entregues podem ser lidas");
        }

        this.status = StatusType.READ;
    }
}
