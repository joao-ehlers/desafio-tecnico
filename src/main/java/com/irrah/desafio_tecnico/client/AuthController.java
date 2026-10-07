package com.irrah.desafio_tecnico.client;

import com.irrah.desafio_tecnico.client.dto.AuthRequest;
import com.irrah.desafio_tecnico.client.dto.AuthResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Autenticação", description = "Identificação simplificada do cliente pelo documento")
@RequiredArgsConstructor
@RestController
@RequestMapping("/auth")
public class AuthController {

    private final ClientService clientService;

    @Operation(
            summary = "Identificar cliente",
            description = "Identificação simplificada. Não cria sessão nem token e não precisa ser chamado antes de cada envio."
    )
    @PostMapping()
    public ResponseEntity<AuthResponse> authenticate(@Valid @RequestBody AuthRequest authRequest){
        return ResponseEntity.ok(clientService.authenticate(authRequest));
    }
}
