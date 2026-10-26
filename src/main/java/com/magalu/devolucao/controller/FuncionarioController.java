package com.magalu.devolucao.controller;

import com.magalu.devolucao.dto.ConferenciaResponse;
import com.magalu.devolucao.dto.DefinirDestinoRequest;
import com.magalu.devolucao.dto.EscaneamentoRequest;
import com.magalu.devolucao.dto.ReprovarDevolucaoRequest;
import com.magalu.devolucao.dto.SolicitacaoResponse;
import com.magalu.devolucao.service.DevolucaoService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Endpoints consumidos pelo PAINEL DO FUNCIONARIO na loja fisica.
 * SecurityConfig exige perfil FUNCIONARIO e o service valida a loja.
 */
@RestController
@RequestMapping("/api/v1/funcionario/devolucoes")
@RequiredArgsConstructor
public class FuncionarioController {

    private final DevolucaoService devolucaoService;

    @GetMapping("/{id}")
    public ResponseEntity<ConferenciaResponse> buscarParaConferencia(@PathVariable Long id) {
        return ResponseEntity.ok(devolucaoService.buscarParaConferencia(id));
    }

    @PostMapping("/escanear")
    public ResponseEntity<SolicitacaoResponse> escanear(@Valid @RequestBody EscaneamentoRequest req) {
        var solicitacao = devolucaoService.processarEscaneamento(req.token());
        return ResponseEntity.ok(solicitacao);
    }

    @PostMapping("/{id}/destino")
    public ResponseEntity<SolicitacaoResponse> definirDestino(
            @PathVariable Long id, @Valid @RequestBody DefinirDestinoRequest req) {
        var solicitacao = devolucaoService.definirDestinoFinal(id, req);
        return ResponseEntity.ok(solicitacao);
    }

    @PostMapping("/{id}/reprovar")
    public ResponseEntity<SolicitacaoResponse> reprovar(
            @PathVariable Long id, @Valid @RequestBody ReprovarDevolucaoRequest req) {
        var solicitacao = devolucaoService.reprovarDevolucao(id, req);
        return ResponseEntity.ok(solicitacao);
    }
}
