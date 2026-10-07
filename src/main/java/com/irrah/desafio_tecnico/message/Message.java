package com.irrah.desafio_tecnico.message;

import com.irrah.desafio_tecnico.client.Client;
import com.irrah.desafio_tecnico.conversation.Conversation;
import com.irrah.desafio_tecnico.message.exception.InvalidMessageStateException;
import com.irrah.desafio_tecnico.shared.exception.InvalidInputException;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.math.BigDecimal;
import java.time.Duration;
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

    @Column(nullable = false)
    private int attempts = 0;

    @Column(name = "next_attempt_at")
    private Instant nextAttemptAt;

    public Message(Conversation conversation, Client sender, String content, Instant timestamp, PriorityType priority, ChannelType channel) {

        if (conversation == null) {
            throw new InvalidInputException("a conversa é obrigatória");
        }

        if (sender == null) {
            throw new InvalidInputException("o remetente é obrigatório");
        }

        if (content == null || content.isBlank()) {
            throw new InvalidInputException("o conteúdo é obrigatório");
        }

        if(content.length() > 2000){
            throw new InvalidInputException("o conteudo pode ter no maximo 2000 caracteres");
        }

        if (timestamp == null) {
            throw new InvalidInputException("a data de criação é obrigatória");
        }

        if (priority == null) {
            throw new InvalidInputException("a prioridade é obrigatória");
        }

        if (channel == null){
            throw new InvalidInputException("o tipo de canal é obrigatório");
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
            throw new InvalidMessageStateException("Somente mensagens enfileiradas podem iniciar o processamento");
        }

        this.status = StatusType.PROCESSING;
        this.attempts++;
        this.nextAttemptAt = null;
    }

    public void markAsSent(){
        if(this.status != StatusType.PROCESSING){
            throw new InvalidMessageStateException("Somente mensagens em processamento podem ser enviadas");
        }

        this.status = StatusType.SENT;
    }

    public void markAsDelivered(){
        if(this.status != StatusType.SENT){
            throw new InvalidMessageStateException("Somente mensagens enviadas podem ser entregues");
        }

        this.status = StatusType.DELIVERED;
    }

    public void markAsFailed(
            Instant now,
            int maxAttempts,
            Duration retryDelay
    ){
        if(this.status != StatusType.PROCESSING){
            throw new InvalidMessageStateException("Somente mensagens em processamento podem  falhar");
        }

        if(now == null){
            throw new InvalidInputException("o horário é obrigatório");
        }

        if(maxAttempts < 1){
            throw new InvalidInputException("o número máximo de tentativas deve ser maior que zero");
        }

        if(retryDelay == null || retryDelay.isNegative() || retryDelay.isZero()){
            throw new InvalidInputException("o delay entre tentativas é obrigatório");
        }

        Instant nextAttempt = (this.attempts < maxAttempts) ?
                now.plus(retryDelay) : null;

        this.status = StatusType.FAILED;
        this.nextAttemptAt = nextAttempt;
    }

    public void markAsRead(){
        if(this.status != StatusType.DELIVERED){
            throw new InvalidMessageStateException("Somente mensagens entregues podem ser lidas");
        }

        this.status = StatusType.READ;
    }

    public void queueForRetry(Instant now){
        if(now == null){
            throw new InvalidInputException("o horário é obrigatório");
        }

        if(this.status != StatusType.FAILED || this.nextAttemptAt == null){
            throw new InvalidMessageStateException("a mensagem não possui nova tentativa agendada");
        }

        if(now.isBefore(this.nextAttemptAt)){
            throw new InvalidMessageStateException("o horário da próxima tentativa ainda não chegou");
        }

        this.status = StatusType.QUEUED;
        this.nextAttemptAt = null;
    }
}
