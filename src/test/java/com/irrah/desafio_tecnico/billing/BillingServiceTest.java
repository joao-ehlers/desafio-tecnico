package com.irrah.desafio_tecnico.billing;

import com.irrah.desafio_tecnico.client.*;
import com.irrah.desafio_tecnico.conversation.Conversation;
import com.irrah.desafio_tecnico.conversation.Recipient;
import com.irrah.desafio_tecnico.message.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.*;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BillingServiceTest {

    // Em São Paulo ainda é 31/10, às 23h.
    private static final Clock CLOCK = Clock.fixed(
            Instant.parse("2026-11-01T02:00:00Z"),
            ZoneId.of("America/Sao_Paulo")
    );

    @Mock
    private FinancialTransactionRepository transactionRepository;

    private BillingService service;

    @BeforeEach
    void setUp() {
        service = new BillingService(CLOCK, transactionRepository);
    }

    @Test
    void shouldDebitPrepaidBalanceAndRegisterCharge() {
        Client client = client(PlanType.PREPAID);
        client.credit(new BigDecimal("10.00"));
        Message message = message(client);

        service.chargeMessage(client, message);

        assertThat(client.getBalance()).isEqualByComparingTo("9.75");
        assertThat(client.getMonthlyConsumption()).isEqualByComparingTo("0");

        assertTransaction(client, message);
    }

    @Test
    void shouldRegisterPostpaidConsumptionUsingBillingTimeZone() {
        Client client = client(PlanType.POSTPAID);
        client.adjustCreditLimit(new BigDecimal("10.00"));
        Message message = message(client);

        service.chargeMessage(client, message);

        assertThat(client.getMonthlyConsumption()).isEqualByComparingTo("0.25");
        assertThat(client.getCreditLimit()).isEqualByComparingTo("10");
        assertThat(client.getBalance()).isEqualByComparingTo("0");

        assertThat(client.getConsumptionMonth())
                .isEqualTo(LocalDate.of(2026, 10, 1));

        assertTransaction(client, message);
    }

    @Test
    void shouldNotRegisterTransactionWhenPostpaidLimitIsInsufficient() {
        Client client = client(PlanType.POSTPAID);
        Message message = message(client); // Limite inicial zero.

        assertThatThrownBy(() -> service.chargeMessage(client, message))
                .isInstanceOf(IllegalStateException.class);

        verifyNoInteractions(transactionRepository);
        assertThat(client.getMonthlyConsumption()).isEqualByComparingTo("0");
        assertThat(client.getConsumptionMonth()).isNull();
    }

    @Test
    void shouldNotRegisterTransactionWhenPrepaidBalanceIsInsufficient() {
        Client client = client(PlanType.PREPAID);
        Message message = message(client);

        assertThatThrownBy(() -> service.chargeMessage(client, message))
                .isInstanceOf(IllegalStateException.class);

        verifyNoInteractions(transactionRepository);
        assertThat(client.getBalance()).isEqualByComparingTo("0");
    }

    private void assertTransaction(Client client, Message message) {
        var captor = ArgumentCaptor.forClass(FinancialTransaction.class);
        verify(transactionRepository).save(captor.capture());

        FinancialTransaction transaction = captor.getValue();

        assertThat(transaction.getClient()).isSameAs(client);
        assertThat(transaction.getMessage()).isSameAs(message);
        assertThat(transaction.getTransactionType()).isEqualTo(TransactionType.DEBIT);
        assertThat(transaction.getAmount()).isEqualByComparingTo(message.getCost());
        assertThat(transaction.getTimestamp()).isEqualTo(message.getTimestamp());
    }

    private Client client(PlanType planType) {
        return new Client(
                "Empresa", "52998224725",
                DocumentType.CPF, planType
        );
    }

    private Message message(Client client) {
        Recipient recipient = new Recipient("Maria", "+5544999990000");
        Conversation conversation = new Conversation(client, recipient);

        return new Message(
                conversation,
                client,
                "Olá",
                Instant.now(CLOCK),
                PriorityType.NORMAL,
                ChannelType.WHATSAPP
        );
    }
}