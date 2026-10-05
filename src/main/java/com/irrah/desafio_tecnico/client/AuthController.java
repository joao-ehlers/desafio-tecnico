package com.irrah.desafio_tecnico.client;

import com.irrah.desafio_tecnico.client.dto.AuthRequest;
import com.irrah.desafio_tecnico.client.dto.AuthResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RequiredArgsConstructor
@RestController
@RequestMapping("/auth")
public class AuthController {

    private final ClientService clientService;

    @PostMapping()
    public ResponseEntity<AuthResponse> authenticate(@Valid @RequestBody AuthRequest authRequest){
        return ResponseEntity.ok(clientService.authenticate(authRequest));
    }
}
