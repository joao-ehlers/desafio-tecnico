package com.irrah.desafio_tecnico.client;

import jakarta.persistence.*;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.*;

import java.math.BigDecimal;

@AllArgsConstructor(access = AccessLevel.PRIVATE)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Getter
@Builder
@Table(name = "clients")
@Entity
public class Client {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank(message = "o nome do cliente é obrigatório")
    @Column(nullable = false)
    private String name;

    @NotBlank(message = "o numero do cnpj/cpf do cliente é obrigatório")
    @Column(name = "document_id", nullable = false, unique = true)
    private String documentId;

    @NotNull(message = "o tipo do documento do cliente é obrigatório")
    @Enumerated(EnumType.STRING)
    @Column(name = "document_type", nullable = false)
    private DocumentType documentType;

    @NotNull(message = "o tipo de plano do cliente é obrigatório")
    @Enumerated(EnumType.STRING)
    @Column(name = "plan_type", nullable = false)
    private PlanType planType;

    @NotNull
    @PositiveOrZero(message = "o saldo não pode ser negativo")
    @Digits(
            integer = 10,
            fraction = 2,
            message = "o saldo deve ter ate 10 digitos inteiros e 2 decimais"
    )
    @Builder.Default
    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal balance = BigDecimal.ZERO;

    @NotNull
    @PositiveOrZero(message = "o saldo não pode ser negativo")
    @Digits(
            integer = 10,
            fraction = 2,
            message = "o saldo deve ter ate 10 digitos inteiros e 2 decimais"
    )
    @Builder.Default
    @Column(name="credit_limit", nullable = false, precision = 12, scale = 2)
    private BigDecimal creditLimit = BigDecimal.ZERO;

    @Builder.Default
    @Column(nullable = false)
    private boolean active = true;

    @Version
    private Long version;
}
