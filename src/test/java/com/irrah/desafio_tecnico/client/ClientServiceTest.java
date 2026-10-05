package com.irrah.desafio_tecnico.client;

import com.irrah.desafio_tecnico.client.dto.AuthRequest;
import com.irrah.desafio_tecnico.client.dto.RegisterRequest;
import com.irrah.desafio_tecnico.client.dto.UpdateRequest;
import com.irrah.desafio_tecnico.client.exception.ClientNotFoundException;
import com.irrah.desafio_tecnico.client.exception.DuplicateDocumentException;
import com.irrah.desafio_tecnico.client.exception.InactiveClientException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;


@ExtendWith(MockitoExtension.class)
class ClientServiceTest {

    private static final Long CLIENT_ID = 1L;
    private static final String CPF = "52998224725";
    private static final String FORMATTED_CPF = "529.982.247-25";

    @Mock
    private ClientRepository clientRepository;

    private ClientService clientService;

    @BeforeEach
    void setUp() {
        clientService = new ClientService(clientRepository);
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
    void shouldRejectPostpaidRegistrationWithoutSaving() {
        var request = registration(CPF, PlanType.POSTPAID);
        when(clientRepository.existsByDocumentId(CPF)).thenReturn(false);

        assertThatThrownBy(() -> clientService.registerClient(request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("pré-pago");

        verify(clientRepository, never()).save(any(Client.class));
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
        var response = responses.get(0);
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
