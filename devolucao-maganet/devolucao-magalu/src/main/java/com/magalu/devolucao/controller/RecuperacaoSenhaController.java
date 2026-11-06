package com.magalu.devolucao.controller;

import com.magalu.devolucao.dto.RedefinirSenhaRequest;
import com.magalu.devolucao.dto.SolicitarRecuperacaoSenhaRequest;
import com.magalu.devolucao.dto.ValidarTokenRequest;
import com.magalu.devolucao.service.RecuperacaoSenhaService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/senha")
@RequiredArgsConstructor
public class RecuperacaoSenhaController {
    private final RecuperacaoSenhaService service;

    @PostMapping("/recuperar")
    public ResponseEntity<Map<String, String>> solicitar(@Valid @RequestBody SolicitarRecuperacaoSenhaRequest request) {
        service.solicitar(request.email());
        // Resposta idêntica exista ou não uma conta com esse endereço.
        return ResponseEntity.accepted().body(Map.of("mensagem",
                "Se o e-mail estiver cadastrado, você receberá as instruções de recuperação."));
    }

    @PostMapping("/validar")
    public Map<String, Boolean> validar(@Valid @RequestBody ValidarTokenRequest request) {
        return Map.of("valido", service.tokenValido(request.token()));
    }

    @PostMapping("/redefinir")
    public ResponseEntity<Map<String, String>> redefinir(@Valid @RequestBody RedefinirSenhaRequest request) {
        try {
            service.redefinir(request.token(), request.novaSenha());
            return ResponseEntity.ok(Map.of("mensagem", "Senha alterada com sucesso. Você já pode entrar com a nova senha."));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("mensagem", e.getMessage()));
        }
    }
}
