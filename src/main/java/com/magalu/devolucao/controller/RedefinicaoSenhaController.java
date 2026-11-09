package com.magalu.devolucao.controller;

import com.magalu.devolucao.dto.RedefinicaoConfirmarRequest;
import com.magalu.devolucao.dto.RedefinicaoSolicitarRequest;
import com.magalu.devolucao.service.RedefinicaoSenhaService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Rotas publicas de recuperacao de senha (o CSRF continua exigido). */
@RestController
@RequestMapping("/api/v1/redefinicao")
@RequiredArgsConstructor
public class RedefinicaoSenhaController {

    private final RedefinicaoSenhaService redefinicao;

    /** Resposta identica exista ou nao uma conta com o e-mail. */
    @PostMapping("/solicitar")
    public ResponseEntity<Void> solicitar(@RequestBody RedefinicaoSolicitarRequest req) {
        redefinicao.solicitar(req.email());
        return ResponseEntity.accepted().build();
    }

    @PostMapping("/confirmar")
    public ResponseEntity<Void> confirmar(@RequestBody RedefinicaoConfirmarRequest req) {
        redefinicao.confirmar(req.token(), req.novaSenha());
        return ResponseEntity.noContent().build();
    }
}
