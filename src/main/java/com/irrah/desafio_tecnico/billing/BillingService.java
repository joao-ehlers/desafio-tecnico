package com.irrah.desafio_tecnico.billing;

import com.irrah.desafio_tecnico.client.Client;
import com.irrah.desafio_tecnico.client.ClientRepository;
import com.irrah.desafio_tecnico.message.Message;
import com.irrah.desafio_tecnico.message.MessageRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.LocalDate;

@RequiredArgsConstructor
@Service
public class BillingService {
    private final Clock clock;
    private final FinancialTransactionRepository financialTransactionRepository;

    public void chargeMessage(Client client, Message message){
        LocalDate referenceMonth = LocalDate.now(clock).withDayOfMonth(1);
        switch(client.getPlanType()){
            case PREPAID -> client.debit(message.getCost());
            case POSTPAID -> client.consumeCredit(message.getCost(), referenceMonth);
        }

        FinancialTransaction financialTransaction = new FinancialTransaction(client, TransactionType.DEBIT, message,
                message.getCost(), message.getTimestamp());

        financialTransactionRepository.save(financialTransaction);

    }
}
