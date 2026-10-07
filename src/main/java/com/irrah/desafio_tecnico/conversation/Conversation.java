package com.irrah.desafio_tecnico.conversation;

import com.irrah.desafio_tecnico.client.Client;
import com.irrah.desafio_tecnico.shared.exception.InvalidInputException;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.*;


@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Getter
@Entity
@Table(name = "conversations")
public class Conversation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull(message = "o cliente da mensagem é obrigatorio")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "client_id", nullable = false)
    private Client client;

    @NotNull(message = "o destinatario da mensagem é obrigatorio")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "recipient_id", nullable = false)
    private Recipient recipient;



    public Conversation(Client client, Recipient recipient) {

        if (client == null) {
            throw new InvalidInputException("o cliente é obrigatório");
        }

        if (recipient == null) {
            throw new InvalidInputException("o destinatário é obrigatório");
        }

        this.client = client;
        this.recipient = recipient;
    }



}
