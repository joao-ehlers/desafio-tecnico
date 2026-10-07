package com.irrah.desafio_tecnico.client;

import com.irrah.desafio_tecnico.billing.FinancialTransaction;
import com.irrah.desafio_tecnico.billing.FinancialTransactionRepository;
import com.irrah.desafio_tecnico.billing.TransactionType;
import com.irrah.desafio_tecnico.billing.exception.WrongConsumingMonthException;
import com.irrah.desafio_tecnico.client.dto.*;
import com.irrah.desafio_tecnico.client.exception.ClientNotFoundException;
import com.irrah.desafio_tecnico.client.exception.DuplicateDocumentException;
import com.irrah.desafio_tecnico.client.exception.InactiveClientException;
import com.irrah.desafio_tecnico.client.validation.DocumentValidator;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

@RequiredArgsConstructor
@Service
public class ClientService {

    private final ClientRepository clientRepository;
    private final FinancialTransactionRepository financialTransactionRepository;
    private final Clock clock;

    public AuthResponse authenticate(AuthRequest request) {
        String document = DocumentValidator.normalizeAndValidate(
                request.documentId()
        );

        Client client = clientRepository.findByDocumentId(document)
                .orElseThrow(ClientNotFoundException::new);

        if (!client.isActive()) {
            throw new InactiveClientException();
        }

        return AuthResponse.builder()
                .clientId(client.getId())
                .build();
    }

    public List<ClientResponse> listAll(){
        List<Client> clients = clientRepository.findAll();

        return clients.stream().map(this::toResponse).toList();
    }

    public ClientResponse getClient(Long id){
        Client client = clientRepository.findById(id).orElseThrow(ClientNotFoundException::new);

        return toResponse(client);
    }


    public BalanceResponse getBalance(Long clientId) {
        Client client = clientRepository.findById(clientId).orElseThrow(ClientNotFoundException::new);

        return switch(client.getPlanType()){
            case PREPAID -> BalanceResponse.builder().balance(client.getBalance()).build();

            case POSTPAID -> {
                LocalDate referenceMonth = LocalDate.now(clock)
                        .withDayOfMonth(1);

                LocalDate consumptionMonth = client.getConsumptionMonth();

                if (consumptionMonth != null
                        && consumptionMonth.isAfter(referenceMonth)) {
                        throw new WrongConsumingMonthException(
                            "o mês de consumo registrado está no futuro"
                    );
                }

                BigDecimal consumption =
                        consumptionMonth == null
                                || consumptionMonth.isBefore(referenceMonth)
                                ? BigDecimal.ZERO
                                : client.getMonthlyConsumption();

                BigDecimal available = client.getCreditLimit()
                        .subtract(consumption)
                        .max(BigDecimal.ZERO);

                yield BalanceResponse.builder()
                        .creditLimit(client.getCreditLimit())
                        .monthlyConsumption(consumption)
                        .available(available)
                        .build();
            }
        };

    }

    @Transactional
    public RegisterResponse registerClient(RegisterRequest request){

        String document = DocumentValidator.normalizeAndValidate(
                request.documentId(),
                request.documentType()
        );

        if (clientRepository.existsByDocumentId(document)) {
            throw new DuplicateDocumentException();
        }

        Client client = new Client(
                request.name(),
                document,
                request.documentType(),
                request.planType()
        );

        clientRepository.save(client);

        return RegisterResponse.builder().clientId(client.getId()).build();
    }

    @Transactional
    public UpdateResponse updateClient(Long clientId, UpdateRequest request) {
        Client target = clientRepository.findById(clientId).orElseThrow(ClientNotFoundException::new);

        String document = DocumentValidator.normalizeAndValidate(
                request.documentId(),
                request.documentType()
        );

        if (clientRepository.existsByDocumentIdAndIdNot(document, clientId)) {
            throw new DuplicateDocumentException();
        }

        target.updateRegistration(
                request.name(),
                document,
                request.documentType()
        );
        clientRepository.save(target);

        return UpdateResponse.builder().clientId(clientId).build();
    }

    @Transactional
    public CreditResponse addCredit(Long id, CreditRequest request){
        Client target = clientRepository.findById(id).orElseThrow(ClientNotFoundException::new);

        target.credit(request.amount());

        FinancialTransaction transaction = new FinancialTransaction(
                target,
                TransactionType.CREDIT,
                null,
                request.amount(),
                Instant.now(clock));

        clientRepository.save(target);
        financialTransactionRepository.save(transaction);

        return CreditResponse.builder()
                .clientId(target.getId())
                .transactionId(transaction.getId())
                .transactionType(transaction.getTransactionType())
                .amount(request.amount())
                .balance(target.getBalance())
                .build();
    }

    private ClientResponse toResponse(Client client) {
        return ClientResponse.builder()
                .id(client.getId())
                .name(client.getName())
                .documentId(client.getDocumentId())
                .documentType(client.getDocumentType().name())
                .planType(client.getPlanType().name())
                .balance(client.getBalance())
                .limit(client.getCreditLimit())
                .active(client.isActive())
                .build();
    }

    @Transactional
    public LimitResponse newLimit(Long id, LimitRequest request){
        Client target = clientRepository.findById(id).orElseThrow(ClientNotFoundException::new);

        target.adjustCreditLimit(request.newLimit());

        clientRepository.save(target);

        return LimitResponse.builder().creditLimit(target.getCreditLimit()).clientId(target.getId()).build();
    }
}
