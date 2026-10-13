package com.magalu.devolucao.controller;

import com.magalu.devolucao.dto.DefinirDestinoRequest;
import com.magalu.devolucao.dto.EscaneamentoRequest;
import com.magalu.devolucao.dto.SolicitacaoResponse;
import com.magalu.devolucao.service.DevolucaoService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * Endpoints consumidos pelo PAINEL DO FUNCIONARIO na loja fisica.
 * SecurityConfig exige perfil FUNCIONARIO e o service valida a loja.
 */
@RestController
@RequestMapping("/api/v1/funcionario/devolucoes")
@RequiredArgsConstructor
public class FuncionarioController {

    private final DevolucaoService devolucaoService;

    @PostMapping("/escanear")
    public ResponseEntity<SolicitacaoResponse> escanear(@Valid @RequestBody EscaneamentoRequest req) {
        var solicitacao = devolucaoService.processarEscaneamento(req.token());
        return ResponseEntity.ok(solicitacao);
    }

    @PostMapping("/{id}/destino")
    public ResponseEntity<SolicitacaoResponse> definirDestino(
            @PathVariable Long id, @Valid @RequestBody DefinirDestinoRequest req) {
        var solicitacao = devolucaoService.definirDestinoFinal(id, req.destino());
        return ResponseEntity.ok(solicitacao);
    }
}
