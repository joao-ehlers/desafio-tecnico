package com.irrah.desafio_tecnico.client;

import com.irrah.desafio_tecnico.client.dto.*;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.List;

@RequiredArgsConstructor
@RestController
@RequestMapping("/clients")
public class ClientController {

    private final ClientService clientService;

    @GetMapping
    public ResponseEntity<List<ClientResponse>> listClients(){
        return ResponseEntity.ok(clientService.listAll());
    }

    @GetMapping("/{id}")
    public ResponseEntity<ClientResponse> getClient(@PathVariable Long id){
        return ResponseEntity.ok(clientService.getClient(id));
    }

    @GetMapping("/{id}/balance")
    public ResponseEntity<BalanceResponse> getBalance(@PathVariable Long id){
        return ResponseEntity.ok(clientService.getBalance(id));
    }

    @PostMapping
    public ResponseEntity<RegisterResponse> registerClient(@Valid @RequestBody RegisterRequest request){
        RegisterResponse response = clientService.registerClient(request);
        return ResponseEntity.created(URI.create("/clients/"+response.clientId())).body(response);
    }

    @PutMapping("/{id}")
    public ResponseEntity<UpdateResponse> updateClient(@PathVariable Long id, @Valid @RequestBody UpdateRequest request){
        return ResponseEntity.ok(clientService.updateClient(id, request));
    }
}
