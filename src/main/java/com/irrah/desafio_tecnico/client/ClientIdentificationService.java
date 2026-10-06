package com.irrah.desafio_tecnico.client;

import com.irrah.desafio_tecnico.client.dto.AuthRequest;
import com.irrah.desafio_tecnico.client.dto.AuthResponse;
import com.irrah.desafio_tecnico.client.exception.ClientNotFoundException;
import com.irrah.desafio_tecnico.client.exception.InactiveClientException;
import com.irrah.desafio_tecnico.client.validation.DocumentValidator;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@RequiredArgsConstructor
@Service
public class ClientIdentificationService {

    private final ClientRepository clientRepository;

    public Long identify(String document) {
        String normalizedDocument = DocumentValidator.normalizeAndValidate(document);

        Client client = clientRepository.findByDocumentId(normalizedDocument)
                .orElseThrow(ClientNotFoundException::new);

        if (!client.isActive()) {
            throw new InactiveClientException();
        }

        return client.getId();
    }
}
