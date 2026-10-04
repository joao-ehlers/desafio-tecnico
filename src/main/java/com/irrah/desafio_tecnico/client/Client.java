package com.irrah.desafio_tecnico.client;

import jakarta.persistence.*;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.*;

import java.math.BigDecimal;
import java.math.RoundingMode;

@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Getter
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
    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal balance = BigDecimal.ZERO;

    @NotNull
    @PositiveOrZero(message = "o limite de credito não pode ser negativo")
    @Digits(
            integer = 10,
            fraction = 2,
            message = "o limite de credito deve ter ate 10 digitos inteiros e 2 decimais"
    )
    @Column(name="credit_limit", nullable = false, precision = 12, scale = 2)
    private BigDecimal creditLimit = BigDecimal.ZERO;

    @Column(nullable = false)
    private boolean active = true;

    @Version
    private Long version;

    private static final BigDecimal MAX_BALANCE = new BigDecimal("9999999999.99");

    public Client(String name, String documentId, DocumentType documentType, PlanType planType) {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException(
                    "o nome do cliente é obrigatório"
            );
        }

        if (documentId == null || documentId.isBlank()) {
            throw new IllegalArgumentException(
                    "o documento do cliente é obrigatório"
            );
        }

        if (documentType == null) {
            throw new IllegalArgumentException(
                    "o tipo do documento é obrigatório"
            );
        }

        if (planType == null) {
            throw new IllegalArgumentException(
                    "o tipo de plano é obrigatório"
            );
        }

        this.name = name.strip();
        this.documentId = documentId.strip();
        this.documentType = documentType;
        this.planType = planType;
    }

    public void activate(){
        this.active = true;
    }

    public void deactivate(){
        this.active = false;
    }

    public void credit(BigDecimal amount){
        validateAmount(amount);

        if (!this.active) {
            throw new IllegalStateException(
                    "cliente inativo não pode receber créditos"
            );
        }

        if(this.planType != PlanType.PREPAID){
            throw new IllegalStateException("para essa operacao de credito, a conta deve possuir o plano pre-pago");
        }

        BigDecimal newBalance = this.balance.add(amount);

        if(newBalance.compareTo(MAX_BALANCE) > 0){
            throw new IllegalStateException("o valor excede o maximo de saldo permitido");
        }

        this.balance = newBalance.setScale(2, RoundingMode.UNNECESSARY);
    }

    public void debit(BigDecimal amount){
        validateAmount(amount);

        if(!this.active){
            throw new IllegalStateException("cliente inativado nao pode realizar operacao de debito");
        }

        if(this.planType != PlanType.PREPAID){
            throw new IllegalStateException("para essa operacao de debito, a conta deve possuir o plano pre-pago");
        }

        if(this.balance.compareTo(amount) < 0){
            throw new IllegalStateException("saldo insuficiente");
        }

        this.balance = this.balance.subtract(amount).setScale(2, RoundingMode.UNNECESSARY);
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
    }
}
