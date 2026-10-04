package com.irrah.desafio_tecnico.billing;

import com.irrah.desafio_tecnico.client.Client;
import com.irrah.desafio_tecnico.message.Message;
import jakarta.persistence.*;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;

@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Getter
@Entity
@Table(name = "transactions")
public class FinancialTransaction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull(message = "o cliente é obrigatorio")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "client_id", nullable = false)
    private Client client;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "transaction_type", nullable = false)
    private TransactionType transactionType;

    @NotNull
    @Positive(message = "o valor não pode ser zero ou negativo")
    @Digits(
            integer = 10,
            fraction = 2,
            message = "o valor deve ter ate 10 digitos inteiros e 2 decimais"
    )
    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal amount;

    @NotNull
    @Column(nullable = false)
    private Instant timestamp;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "message_id", unique = true)
    private Message message;

    private static final BigDecimal MAX_AMOUNT =
            new BigDecimal("9999999999.99");

    public FinancialTransaction(Client client, TransactionType transactionType, Message message, BigDecimal amount, Instant timestamp) {
        validateAmount(amount);

        if (client == null) {
            throw new IllegalArgumentException("o cliente é obrigatório");
        }

        if (transactionType == null) {
            throw new IllegalArgumentException("o tipo da transação é obrigatório");
        }

        if (timestamp == null) {
            throw new IllegalArgumentException("o horario da transação é obrigatório");
        }

        if (transactionType == TransactionType.DEBIT) {
            if (message == null) {
                throw new IllegalArgumentException(
                        "o débito por envio deve estar associado a uma mensagem"
                );
            }

            if (amount.compareTo(message.getCost()) != 0) {
                throw new IllegalArgumentException(
                        "o valor do débito deve ser igual ao custo da mensagem"
                );
            }
        }

        if (transactionType == TransactionType.CREDIT && message != null) {
            throw new IllegalArgumentException(
                    "o crédito por recarga não deve estar associado a uma mensagem"
            );
        }

        this.client = client;
        this.transactionType = transactionType;
        this.amount = amount.setScale(2, RoundingMode.UNNECESSARY);
        this.timestamp = timestamp;
        this.message = message;
    }

    private void validateAmount(BigDecimal amount){
        if(amount == null || amount.signum() <= 0){
            throw new IllegalArgumentException(
                    "o valor deve ser maior que zero"
            );
        }

        if(amount.stripTrailingZeros().scale() > 2){
            throw new IllegalArgumentException(
                    "o valor deve possuir no maximo 2 casas decimais"
            );
        }

        if(amount.compareTo(MAX_AMOUNT) > 0){
            throw new IllegalArgumentException("o valor excede o maximo permitido");
        }
    }
}
