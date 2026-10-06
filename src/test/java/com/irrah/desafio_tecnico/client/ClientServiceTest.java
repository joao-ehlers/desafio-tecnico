package com.irrah.desafio_tecnico.client;

import com.irrah.desafio_tecnico.billing.FinancialTransaction;
import com.irrah.desafio_tecnico.billing.FinancialTransactionRepository;
import com.irrah.desafio_tecnico.billing.TransactionType;
import com.irrah.desafio_tecnico.client.dto.*;
import com.irrah.desafio_tecnico.client.exception.ClientNotFoundException;
import com.irrah.desafio_tecnico.client.exception.DuplicateDocumentException;
import com.irrah.desafio_tecnico.client.exception.InactiveClientException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;


@ExtendWith(MockitoExtension.class)
class ClientServiceTest {

    private static final Long CLIENT_ID = 1L;
    private static final String CPF = "52998224725";
    private static final String FORMATTED_CPF = "529.982.247-25";

    @Mock
    private ClientRepository clientRepository;

    private ClientService clientService;

    @Mock
    private FinancialTransactionRepository financialTransactionRepository;

    private final Clock clock = Clock.fixed(
            Instant.parse("2026-11-15T12:00:00Z"),
            ZoneId.of("America/Sao_Paulo")
    );

    @BeforeEach
    void setUp() {
        clientService = new ClientService(
                clientRepository,
                financialTransactionRepository,
                clock
        );
    }

    @ParameterizedTest
    @CsvSource({
            "529.982.247-25, CPF, 52998224725",
            "52998224725, CPF, 52998224725",
            "11.222.333/0001-81, CNPJ, 11222333000181",
            "12.abc.345/01de-35, CNPJ, 12ABC34501DE35"
    })
    void shouldRegisterPrepaidClientWithNormalizedDocument(
            String input, DocumentType type, String normalized
    ) {
        var request = new RegisterRequest(
                "Empresa Exemplo", input, type, PlanType.PREPAID
        );
        when(clientRepository.existsByDocumentId(normalized)).thenReturn(false);

        when(clientRepository.save(any(Client.class))).thenAnswer(invocation -> {
            Client client = invocation.getArgument(0);
            ReflectionTestUtils.setField(client, "id", CLIENT_ID);
            return client;
        });

        var response = clientService.registerClient(request);

        var captor = ArgumentCaptor.forClass(Client.class);
        verify(clientRepository).save(captor.capture());
        Client saved = captor.getValue();

        assertThat(response.clientId()).isEqualTo(CLIENT_ID);
        assertThat(saved.getName()).isEqualTo("Empresa Exemplo");
        assertThat(saved.getDocumentId()).isEqualTo(normalized);
        assertThat(saved.getDocumentType()).isEqualTo(type);
        assertThat(saved.getPlanType()).isEqualTo(PlanType.PREPAID);
        assertThat(saved.isActive()).isTrue();
        assertThat(saved.getBalance()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(saved.getCreditLimit()).isEqualByComparingTo(BigDecimal.ZERO);
        verify(clientRepository).existsByDocumentId(normalized);
    }

    @Test
    void shouldRejectDuplicateDocumentWithoutSaving() {
        var request = registration(FORMATTED_CPF, PlanType.PREPAID);
        when(clientRepository.existsByDocumentId(CPF)).thenReturn(true);

        assertThatThrownBy(() -> clientService.registerClient(request))
                .isInstanceOf(DuplicateDocumentException.class);

        verify(clientRepository, never()).save(any(Client.class));
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {" ", "111.111.111-11", "529.982.247-26", "529!98224725"})
    void shouldRejectInvalidDocumentBeforeAccessingRepository(String document) {
        var request = registration(document, PlanType.PREPAID);

        assertThatThrownBy(() -> clientService.registerClient(request))
                .isInstanceOf(IllegalArgumentException.class);

        verifyNoInteractions(clientRepository);
    }

    @Test
    void shouldRejectDocumentThatDoesNotMatchItsType() {
        var request = new RegisterRequest(
                "Empresa Exemplo", CPF, DocumentType.CNPJ, PlanType.PREPAID
        );

        assertThatThrownBy(() -> clientService.registerClient(request))
                .isInstanceOf(IllegalArgumentException.class);

        verifyNoInteractions(clientRepository);
    }

    @Test
    void shouldPropagateDatabaseIntegrityFailure() {
        var request = registration(CPF, PlanType.PREPAID);
        when(clientRepository.existsByDocumentId(CPF)).thenReturn(false);
        var failure = new DataIntegrityViolationException("unique constraint");
        when(clientRepository.save(any(Client.class))).thenThrow(failure);

        assertThatThrownBy(() -> clientService.registerClient(request))
                .isSameAs(failure);
    }

    @Test
    void shouldUpdateRegistrationKeepingOwnDocumentAndFinancialState() {
        Client target = existingClient();
        target.credit(new BigDecimal("10.00"));
        target.deactivate();
        var request = new UpdateRequest("Novo nome", FORMATTED_CPF, DocumentType.CPF);
        when(clientRepository.findById(CLIENT_ID)).thenReturn(Optional.of(target));
        when(clientRepository.existsByDocumentIdAndIdNot(CPF, CLIENT_ID))
                .thenReturn(false);

        var response = clientService.updateClient(CLIENT_ID, request);

        assertThat(response.clientId()).isEqualTo(CLIENT_ID);
        assertThat(target.getName()).isEqualTo("Novo nome");
        assertThat(target.getDocumentId()).isEqualTo(CPF);
        assertThat(target.getDocumentType()).isEqualTo(DocumentType.CPF);
        assertThat(target.getPlanType()).isEqualTo(PlanType.PREPAID);
        assertThat(target.getBalance()).isEqualByComparingTo("10.00");
        assertThat(target.isActive()).isFalse();
        verify(clientRepository).existsByDocumentIdAndIdNot(CPF, CLIENT_ID);
        verify(clientRepository).save(target);
    }

    @Test
    void shouldUpdateToAnotherValidNormalizedDocument() {
        Client target = existingClient();
        var request = new UpdateRequest(
                "Novo nome", "111.444.777-35", DocumentType.CPF
        );
        when(clientRepository.findById(CLIENT_ID)).thenReturn(Optional.of(target));
        when(clientRepository.existsByDocumentIdAndIdNot("11144477735", CLIENT_ID))
                .thenReturn(false);

        clientService.updateClient(CLIENT_ID, request);

        assertThat(target.getDocumentId()).isEqualTo("11144477735");
        verify(clientRepository).save(target);
    }

    @Test
    void shouldRejectDuplicateDocumentOnUpdateWithoutChangingClient() {
        Client target = existingClient();
        var request = new UpdateRequest(
                "Novo nome", "111.444.777-35", DocumentType.CPF
        );
        when(clientRepository.findById(CLIENT_ID)).thenReturn(Optional.of(target));
        when(clientRepository.existsByDocumentIdAndIdNot("11144477735", CLIENT_ID))
                .thenReturn(true);

        assertThatThrownBy(() -> clientService.updateClient(CLIENT_ID, request))
                .isInstanceOf(DuplicateDocumentException.class);

        assertThat(target.getName()).isEqualTo("Cliente Exemplo");
        assertThat(target.getDocumentId()).isEqualTo(CPF);
        verify(clientRepository, never()).save(any(Client.class));
    }

    @Test
    void shouldRejectUpdateWhenClientDoesNotExist() {
        var request = new UpdateRequest("Novo nome", FORMATTED_CPF, DocumentType.CPF);
        when(clientRepository.findById(CLIENT_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> clientService.updateClient(CLIENT_ID, request))
                .isInstanceOf(ClientNotFoundException.class);

        verify(clientRepository, never()).save(any(Client.class));
    }

    @Test
    void shouldRejectInvalidDocumentOnUpdateWithoutChangingClient() {
        Client target = existingClient();
        var request = new UpdateRequest("Novo nome", "52998224726", DocumentType.CPF);
        when(clientRepository.findById(CLIENT_ID)).thenReturn(Optional.of(target));

        assertThatThrownBy(() -> clientService.updateClient(CLIENT_ID, request))
                .isInstanceOf(IllegalArgumentException.class);

        assertThat(target.getName()).isEqualTo("Cliente Exemplo");
        assertThat(target.getDocumentId()).isEqualTo(CPF);
        verify(clientRepository, never()).save(any(Client.class));
    }

    @Test
    void shouldAuthenticateUsingNormalizedDocument() {
        Client client = existingClient();
        when(clientRepository.findByDocumentId(CPF)).thenReturn(Optional.of(client));

        var response = clientService.authenticate(new AuthRequest(FORMATTED_CPF));

        assertThat(response.clientId()).isEqualTo(CLIENT_ID);
        verify(clientRepository).findByDocumentId(CPF);
    }

    @Test
    void shouldRejectAuthenticationOfUnknownClient() {
        when(clientRepository.findByDocumentId(CPF)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> clientService.authenticate(new AuthRequest(FORMATTED_CPF)))
                .isInstanceOf(ClientNotFoundException.class);
    }

    @Test
    void shouldRejectAuthenticationOfInactiveClient() {
        Client client = existingClient();
        client.deactivate();
        when(clientRepository.findByDocumentId(CPF)).thenReturn(Optional.of(client));

        assertThatThrownBy(() -> clientService.authenticate(new AuthRequest(FORMATTED_CPF)))
                .isInstanceOf(InactiveClientException.class);
    }

    @Test
    void shouldRejectAuthenticationWithInvalidDocumentBeforeQueryingRepository() {
        assertThatThrownBy(() -> clientService.authenticate(new AuthRequest("11111111111")))
                .isInstanceOf(IllegalArgumentException.class);

        verifyNoInteractions(clientRepository);
    }

    @Test
    void shouldReturnEmptyListWhenNoClientsExist() {
        when(clientRepository.findAll()).thenReturn(List.of());

        assertThat(clientService.listAll()).isEmpty();
    }

    @Test
    void shouldListClientsWithTheirFinancialAndRegistrationData() {
        Client client = existingClient();
        client.credit(new BigDecimal("12.50"));
        when(clientRepository.findAll()).thenReturn(List.of(client));

        var responses = clientService.listAll();

        assertThat(responses).hasSize(1);
        var response = responses.getFirst();
        assertThat(response.id()).isEqualTo(CLIENT_ID);
        assertThat(response.name()).isEqualTo("Cliente Exemplo");
        assertThat(response.documentId()).isEqualTo(CPF);
        assertThat(response.documentType()).isEqualTo("CPF");
        assertThat(response.planType()).isEqualTo("PREPAID");
        assertThat(response.active()).isTrue();
        assertThat(response.balance()).isEqualByComparingTo("12.50");
        assertThat(response.limit()).isEqualByComparingTo(BigDecimal.ZERO);
    }

    @Test
    void shouldFindClientById() {
        when(clientRepository.findById(CLIENT_ID)).thenReturn(Optional.of(existingClient()));

        assertThat(clientService.getClient(CLIENT_ID).id()).isEqualTo(CLIENT_ID);
    }

    @Test
    void shouldRejectLookupOfUnknownClient() {
        when(clientRepository.findById(CLIENT_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> clientService.getClient(CLIENT_ID))
                .isInstanceOf(ClientNotFoundException.class);
    }

    @Test
    void shouldReturnCurrentBalance() {
        Client client = existingClient();
        client.credit(new BigDecimal("12.50"));
        when(clientRepository.findById(CLIENT_ID)).thenReturn(Optional.of(client));

        assertThat(clientService.getBalance(CLIENT_ID).balance())
                .isEqualByComparingTo("12.50");
    }

    @Test
    void shouldRejectBalanceLookupOfUnknownClient() {
        when(clientRepository.findById(CLIENT_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> clientService.getBalance(CLIENT_ID))
                .isInstanceOf(ClientNotFoundException.class);
    }

    @Test
    void shouldCreditClientAndRegisterFinancialTransaction() {
        Client client = existingClient();
        client.credit(new BigDecimal("10.00"));

        when(clientRepository.findById(CLIENT_ID))
                .thenReturn(Optional.of(client));

        when(financialTransactionRepository.save(
                any(FinancialTransaction.class)
        )).thenAnswer(invocation -> {
            FinancialTransaction transaction = invocation.getArgument(0);
            ReflectionTestUtils.setField(transaction, "id", 100L);
            return transaction;
        });

        var response = clientService.addCredit(
                CLIENT_ID,
                new CreditRequest(new BigDecimal("50.00"))
        );

        assertThat(client.getBalance()).isEqualByComparingTo("60.00");
        assertThat(response.balance()).isEqualByComparingTo("60.00");
        assertThat(response.amount()).isEqualByComparingTo("50.00");
        assertThat(response.clientId()).isEqualTo(CLIENT_ID);
        assertThat(response.transactionId()).isEqualTo(100L);
        assertThat(response.transactionType()).isEqualTo(TransactionType.CREDIT);

        var captor = ArgumentCaptor.forClass(FinancialTransaction.class);
        verify(financialTransactionRepository).save(captor.capture());

        FinancialTransaction transaction = captor.getValue();

        assertThat(transaction.getClient()).isSameAs(client);
        assertThat(transaction.getTransactionType())
                .isEqualTo(TransactionType.CREDIT);
        assertThat(transaction.getAmount()).isEqualByComparingTo("50.00");
        assertThat(transaction.getMessage()).isNull();
        assertThat(transaction.getTimestamp()).isNotNull();
    }

    @Test
    void shouldRejectCreditForUnknownClient() {
        when(clientRepository.findById(CLIENT_ID))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> clientService.addCredit(
                CLIENT_ID,
                new CreditRequest(new BigDecimal("50.00"))
        )).isInstanceOf(ClientNotFoundException.class);

        verifyNoInteractions(financialTransactionRepository);
        verify(clientRepository, never()).save(any(Client.class));
    }

    @Test
    void shouldRejectCreditForInactiveClient() {
        Client client = existingClient();
        client.deactivate();

        when(clientRepository.findById(CLIENT_ID))
                .thenReturn(Optional.of(client));

        assertThatThrownBy(() -> clientService.addCredit(
                CLIENT_ID,
                new CreditRequest(new BigDecimal("50.00"))
        )).isInstanceOf(IllegalStateException.class);

        assertThat(client.getBalance()).isEqualByComparingTo("0.00");
        verifyNoInteractions(financialTransactionRepository);
        verify(clientRepository, never()).save(any(Client.class));
    }

    @ParameterizedTest
    @NullSource
    @ValueSource(strings = {"0.00", "-1.00", "0.001"})
    void shouldRejectInvalidCreditAmount(String value) {
        Client client = existingClient();

        when(clientRepository.findById(CLIENT_ID))
                .thenReturn(Optional.of(client));

        BigDecimal amount = value == null ? null : new BigDecimal(value);

        assertThatThrownBy(() -> clientService.addCredit(
                CLIENT_ID,
                new CreditRequest(amount)
        )).isInstanceOf(IllegalArgumentException.class);

        assertThat(client.getBalance()).isEqualByComparingTo("0.00");
        verifyNoInteractions(financialTransactionRepository);
        verify(clientRepository, never()).save(any(Client.class));
    }

    @Test
    void shouldRejectCreditAboveMaximumBalance() {
        Client client = existingClient();
        client.credit(new BigDecimal("9999999999.99"));

        when(clientRepository.findById(CLIENT_ID))
                .thenReturn(Optional.of(client));

        assertThatThrownBy(() -> clientService.addCredit(
                CLIENT_ID,
                new CreditRequest(new BigDecimal("0.01"))
        )).isInstanceOf(IllegalStateException.class);

        assertThat(client.getBalance())
                .isEqualByComparingTo("9999999999.99");

        verifyNoInteractions(financialTransactionRepository);
        verify(clientRepository, never()).save(any(Client.class));
    }

    @Test
    void shouldPropagateFailureWhenFinancialTransactionCannotBeSaved() {
        Client client = existingClient();

        when(clientRepository.findById(CLIENT_ID))
                .thenReturn(Optional.of(client));

        var failure = new DataIntegrityViolationException(
                "simulated persistence failure"
        );

        when(financialTransactionRepository.save(
                any(FinancialTransaction.class)
        )).thenThrow(failure);

        assertThatThrownBy(() -> clientService.addCredit(
                CLIENT_ID,
                new CreditRequest(new BigDecimal("50.00"))
        )).isSameAs(failure);
    }

    @Test
    void shouldReturnFullLimitBeforeFirstPostpaidConsumption() {
        Client client = postpaidClient("100.00");
        when(clientRepository.findById(1L)).thenReturn(Optional.of(client));

        var response = clientService.getBalance(1L);

        assertThat(response.creditLimit()).isEqualByComparingTo("100");
        assertThat(response.monthlyConsumption()).isEqualByComparingTo("0");
        assertThat(response.available()).isEqualByComparingTo("100");
    }

    @Test
    void shouldReturnCurrentMonthConsumptionAndAvailableLimit() {
        Client client = postpaidClient("100.00");
        client.consumeCredit(
                new BigDecimal("40.00"),
                LocalDate.of(2026, 11, 1)
        );

        when(clientRepository.findById(1L)).thenReturn(Optional.of(client));

        var response = clientService.getBalance(1L);

        assertThat(response.monthlyConsumption()).isEqualByComparingTo("40");
        assertThat(response.available()).isEqualByComparingTo("60");
    }

    @Test
    void shouldIgnorePreviousMonthConsumptionWithoutMutatingClient() {
        Client client = postpaidClient("100.00");
        LocalDate october = LocalDate.of(2026, 10, 1);
        client.consumeCredit(new BigDecimal("40.00"), october);

        when(clientRepository.findById(1L)).thenReturn(Optional.of(client));

        var response = clientService.getBalance(1L);

        assertThat(response.monthlyConsumption()).isEqualByComparingTo("0");
        assertThat(response.available()).isEqualByComparingTo("100");

        assertThat(client.getMonthlyConsumption()).isEqualByComparingTo("40");
        assertThat(client.getConsumptionMonth()).isEqualTo(october);
        verify(clientRepository, never()).save(any(Client.class));
    }

    @Test
    void shouldReturnZeroAvailableWhenLimitIsBelowConsumption() {
        Client client = postpaidClient("100.00");
        client.consumeCredit(
                new BigDecimal("40.00"),
                LocalDate.of(2026, 11, 1)
        );
        client.adjustCreditLimit(new BigDecimal("20.00"));

        when(clientRepository.findById(1L)).thenReturn(Optional.of(client));

        var response = clientService.getBalance(1L);

        assertThat(response.monthlyConsumption()).isEqualByComparingTo("40");
        assertThat(response.available()).isEqualByComparingTo("0");
    }

    @Test
    void shouldRejectBalanceQueryWhenStoredConsumptionMonthIsInTheFuture() {
        Client client = postpaidClient("100.00");
        client.consumeCredit(
                new BigDecimal("1.00"),
                LocalDate.of(2026, 12, 1)
        );

        when(clientRepository.findById(1L)).thenReturn(Optional.of(client));

        assertThatThrownBy(() -> clientService.getBalance(1L))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void shouldUpdateLimitWithoutRegisteringFinancialTransaction() {
        Client client = postpaidClient("100.00");
        when(clientRepository.findById(1L)).thenReturn(Optional.of(client));

        LimitRequest request = mock(LimitRequest.class);
        when(request.newLimit()).thenReturn(new BigDecimal("200.00"));

        clientService.newLimit(1L, request);

        assertThat(client.getCreditLimit()).isEqualByComparingTo("200");
        verifyNoInteractions(financialTransactionRepository);
    }

    private Client postpaidClient(String limit) {
        Client client = new Client(
                "Empresa", "52998224725",
                DocumentType.CPF, PlanType.POSTPAID
        );
        client.adjustCreditLimit(new BigDecimal(limit));
        return client;
    }

    private RegisterRequest registration(String document, PlanType plan) {
        return new RegisterRequest("Cliente Exemplo", document, DocumentType.CPF, plan);
    }

    private Client existingClient() {
        Client client = new Client(
                "Cliente Exemplo", CPF, DocumentType.CPF, PlanType.PREPAID
        );
        ReflectionTestUtils.setField(client, "id", CLIENT_ID);
        return client;
    }
}
