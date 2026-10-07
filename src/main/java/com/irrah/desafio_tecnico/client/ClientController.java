package com.irrah.desafio_tecnico.client;

import com.irrah.desafio_tecnico.client.dto.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.List;

@Tag(name = "Clientes", description = "Cadastro, consulta, saldo, recarga e limite de crédito")
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

    @Operation(
            summary = "Consultar saldo do cliente",
            description = "Retorna os dados de saldo conforme o plano. Campos que não se aplicam ao plano (pré ou pós-pago) podem vir como null."
    )
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

    @PostMapping("/{id}/credits")
    public ResponseEntity<CreditResponse> addCredit(@PathVariable Long id, @Valid @RequestBody CreditRequest request){
        return ResponseEntity.status(201).body(clientService.addCredit(id, request));
    }

    @Operation(
            summary = "Definir limite de crédito",
            description = "Define o limite total do cliente (não é recarga). Aceita zero."
    )
    @PutMapping("/{id}/credit-limit")
    public ResponseEntity<LimitResponse> newLimit(@PathVariable Long id, @Valid @RequestBody LimitRequest request){
        return ResponseEntity.ok(clientService.newLimit(id, request));
    }

    @PutMapping("/{id}/plan-switch")
    public ResponseEntity<SwitchResponse> planSwitch(@PathVariable Long id){
        return ResponseEntity.ok(clientService.planSwitch(id));
    }
}
