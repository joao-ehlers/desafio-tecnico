package com.irrah.desafio_tecnico.message;

import com.irrah.desafio_tecnico.billing.FinancialTransactionRepository;
import com.irrah.desafio_tecnico.client.*;
import com.irrah.desafio_tecnico.conversation.*;
import com.irrah.desafio_tecnico.message.dto.NewMessageRequest;
import com.irrah.desafio_tecnico.message.exception.MessageDeliveryException;
import com.irrah.desafio_tecnico.queue.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.math.BigDecimal;
import java.time.*;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@SpringBootTest(properties = {
        "spring.jpa.hibernate.ddl-auto=validate",
        "spring.jpa.open-in-view=false",
        "spring.flyway.enabled=true"
})
@Testcontainers
class AsyncBillingIntegrationTest {
    @Container
    static final PostgreSQLContainer<?> POSTGRES =
            new PostgreSQLContainer<>("postgres:16-alpine")
                    .withDatabaseName("bcb_test")
                    .withUsername("test")
                    .withPassword("test");

    @DynamicPropertySource
    static void databaseProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
        registry.add("spring.flyway.url", POSTGRES::getJdbcUrl);
        registry.add("spring.flyway.user", POSTGRES::getUsername);
        registry.add("spring.flyway.password", POSTGRES::getPassword);
    }

    // Replace only automatic execution: real processing services are invoked below.
    @MockitoBean MessageQueueWorker scheduledWorker;
    @MockitoBean MessageSender sender;
    @MockitoBean Clock clock;

    @Autowired MessageService messageService;
    @Autowired MessageProcessingService processing;
    @Autowired MessageRetryService retries;
    @Autowired MessageRepository messages;
    @Autowired ClientRepository clients;
    @Autowired ConversationRepository conversations;
    @Autowired RecipientRepository recipients;
    @Autowired FinancialTransactionRepository transactions;
    @Autowired InMemoryMessageQueue queue;
    @Autowired QueueMetrics metrics;

    private final AtomicReference<Instant> now =
            new AtomicReference<>(Instant.parse("2026-10-07T12:00:00Z"));

    @BeforeEach
    void setUp() {
        now.set(Instant.parse("2026-10-07T12:00:00Z"));
        when(clock.instant()).thenAnswer(invocation -> now.get());
        when(clock.getZone()).thenReturn(ZoneId.of("America/Sao_Paulo"));
        // Only the disposable Testcontainers database is cleared.
        while (queue.dequeue() != null) { }
        transactions.deleteAll();
        messages.deleteAll();
        conversations.deleteAll();
        recipients.deleteAll();
        clients.deleteAll();
    }

    @ParameterizedTest
    @EnumSource(value = PlanType.class, names = {"PREPAID", "POSTPAID"})
    void shouldRetryWithoutChargingAgain(PlanType plan) {
        Client client = fundedClient(plan);
        var initialMetrics = metrics.snapshot();
        doThrow(new MessageDeliveryException()).doNothing()
                .when(sender).sendMessage(any(Message.class));

        var accepted = messageService.newMessage(client.getId(), request());
        Long id = accepted.messageId();
        assertThat(accepted.statusType()).isEqualTo(StatusType.QUEUED);
        assertThat(transactions.count()).isEqualTo(1);
        assertChargedOnce(client.getId(), plan);

        processing.processPendingMessages();
        Message failed = messages.findById(id).orElseThrow();
        assertThat(failed.getStatus()).isEqualTo(StatusType.FAILED);
        assertThat(failed.getAttempts()).isEqualTo(1);
        Instant due = failed.getNextAttemptAt();
        assertThat(due).isAfter(now.get());
        assertThat(metrics.snapshot().failed()).isEqualTo(initialMetrics.failed());

        now.set(due.minusMillis(1));
        retries.enqueueDueRetries();
        assertThat(queue.size()).isZero();
        assertThat(messages.findById(id).orElseThrow().getAttempts()).isEqualTo(1);

        now.set(due);
        retries.enqueueDueRetries();
        assertThat(queue.size()).isEqualTo(1);
        Message queued = messages.findById(id).orElseThrow();
        assertThat(queued.getStatus()).isEqualTo(StatusType.QUEUED);
        assertThat(queued.getNextAttemptAt()).isNull();
        retries.enqueueDueRetries(); // Already queued: must not enqueue twice.
        assertThat(queue.size()).isEqualTo(1);

        processing.processPendingMessages();
        Message sent = messages.findById(id).orElseThrow();
        assertThat(sent.getStatus()).isEqualTo(StatusType.SENT);
        assertThat(sent.getAttempts()).isEqualTo(2);
        assertThat(sent.getNextAttemptAt()).isNull();
        assertThat(transactions.count()).isEqualTo(1);
        assertChargedOnce(client.getId(), plan);
        assertThat(metrics.snapshot().sent()).isEqualTo(initialMetrics.sent() + 1);
        assertThat(metrics.snapshot().failed()).isEqualTo(initialMetrics.failed());
        verify(sender, times(2)).sendMessage(any(Message.class));
    }

    @Test
    void shouldPersistProcessingBeforeCallingSender() {
        Client client = fundedClient(PlanType.PREPAID);
        doAnswer(invocation -> {
            Message argument = invocation.getArgument(0);
            Message committed = messages.findById(argument.getId()).orElseThrow();
            assertThat(committed.getStatus()).isEqualTo(StatusType.PROCESSING);
            assertThat(committed.getAttempts()).isEqualTo(1);
            return null;
        }).when(sender).sendMessage(any(Message.class));
        var accepted = messageService.newMessage(client.getId(), request());
        processing.processPendingMessages();
        assertThat(messages.findById(accepted.messageId()).orElseThrow().getStatus())
                .isEqualTo(StatusType.SENT);
    }

    @Test
    void shouldEndWithFinalFailureAfterExactlyThreeAttempts() {
        Client client = fundedClient(PlanType.PREPAID);
        var initialMetrics = metrics.snapshot();
        doThrow(new MessageDeliveryException()).when(sender).sendMessage(any(Message.class));
        Long id = messageService.newMessage(client.getId(), request()).messageId();
        for (int attempt = 1; attempt <= 3; attempt++) {
            processing.processPendingMessages();
            Message failed = messages.findById(id).orElseThrow();
            assertThat(failed.getStatus()).isEqualTo(StatusType.FAILED);
            assertThat(failed.getAttempts()).isEqualTo(attempt);
            if (attempt < 3) {
                assertThat(failed.getNextAttemptAt()).isNotNull();
                now.set(failed.getNextAttemptAt());
                retries.enqueueDueRetries();
            } else {
                assertThat(failed.getNextAttemptAt()).isNull();
            }
        }
        now.set(now.get().plusSeconds(3600));
        retries.enqueueDueRetries();
        processing.processPendingMessages();
        assertThat(queue.size()).isZero();
        verify(sender, times(3)).sendMessage(any(Message.class));
        assertThat(transactions.count()).isEqualTo(1);
        assertChargedOnce(client.getId(), PlanType.PREPAID);
        assertThat(metrics.snapshot().failed()).isEqualTo(initialMetrics.failed() + 1);
        assertThat(metrics.snapshot().sent()).isEqualTo(initialMetrics.sent());
    }

    @ParameterizedTest
    @EnumSource(value = PlanType.class, names = {"PREPAID", "POSTPAID"})
    void shouldRollbackRegistrationWhenFundsAreInsufficient(PlanType plan) {
        Client client = clients.saveAndFlush(new Client(
                "Empresa", "52998224725", DocumentType.CPF, plan));
        assertThatThrownBy(() -> messageService.newMessage(client.getId(), request()))
                .isInstanceOf(IllegalStateException.class);
        assertThat(messages.count()).isZero();
        assertThat(transactions.count()).isZero();
        assertThat(conversations.count()).isZero();
        assertThat(recipients.count()).isZero();
        assertThat(queue.size()).isZero();
        Client stored = clients.findById(client.getId()).orElseThrow();
        assertThat(stored.getBalance()).isEqualByComparingTo("0");
        assertThat(stored.getMonthlyConsumption()).isEqualByComparingTo("0");
        verifyNoInteractions(sender);
    }

    private Client fundedClient(PlanType plan) {
        Client client = new Client("Empresa", "52998224725", DocumentType.CPF, plan);
        if (plan == PlanType.PREPAID) client.credit(new BigDecimal("10.00"));
        else client.adjustCreditLimit(new BigDecimal("10.00"));
        // Fixture setup, not a real credit operation: no financial transaction is created here.
        return clients.saveAndFlush(client);
    }

    private NewMessageRequest request() {
        return NewMessageRequest.builder()
                .recipientName("Maria")
                .recipientPhone("+5544999990000")
                .content("Mensagem de integração")
                .priorityType(PriorityType.NORMAL)
                .channelType(ChannelType.WHATSAPP)
                .build();
    }

    private void assertChargedOnce(Long clientId, PlanType plan) {
        Client stored = clients.findById(clientId).orElseThrow();
        if (plan == PlanType.PREPAID) {
            assertThat(stored.getBalance()).isEqualByComparingTo("9.75");
            assertThat(stored.getMonthlyConsumption()).isEqualByComparingTo("0");
        } else {
            assertThat(stored.getMonthlyConsumption()).isEqualByComparingTo("0.25");
            assertThat(stored.getCreditLimit()).isEqualByComparingTo("10");
            assertThat(stored.getConsumptionMonth()).isEqualTo(LocalDate.of(2026, 10, 1));
        }
    }
}
