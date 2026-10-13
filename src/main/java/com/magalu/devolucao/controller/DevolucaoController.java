package com.magalu.devolucao.controller;

import com.magalu.devolucao.dto.*;
import com.magalu.devolucao.service.DevolucaoService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;

@RestController @RequestMapping("/api/v1/devolucoes") @RequiredArgsConstructor
public class DevolucaoController {
    private final DevolucaoService devolucaoService;
    @PostMapping
    public ResponseEntity<SolicitacaoResponse> criarSolicitacao(@Valid @RequestBody CriarSolicitacaoRequest req) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(devolucaoService.criarSolicitacao(req.itemCompradoId(), req.lojaId()));
    }
    @GetMapping("/{id}")
    public SolicitacaoResponse buscarPorId(@PathVariable Long id) { return devolucaoService.buscarPorId(id); }
    @PostMapping("/{id}/qrcode/renovar")
    public SolicitacaoResponse renovar(@PathVariable Long id) { return devolucaoService.renovarQRCode(id); }
    @PostMapping("/{id}/cancelar")
    public SolicitacaoResponse cancelar(@PathVariable Long id) { return devolucaoService.cancelar(id); }
}
