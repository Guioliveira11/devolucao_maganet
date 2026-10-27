package com.magalu.devolucao.controller;

import com.magalu.devolucao.dto.CadastroClienteRequest;
import com.magalu.devolucao.dto.CadastroFuncionarioRequest;
import com.magalu.devolucao.dto.CadastroOngRequest;
import com.magalu.devolucao.dto.CadastroOpcoesResponse;
import com.magalu.devolucao.service.CadastroService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Rotas publicas de cadastro (o CSRF continua exigido nas escritas). */
@RestController
@RequestMapping("/api/v1/cadastro")
@RequiredArgsConstructor
public class CadastroController {

    private final CadastroService cadastro;

    @GetMapping("/opcoes")
    public CadastroOpcoesResponse opcoes() {
        return cadastro.opcoes();
    }

    @PostMapping("/cliente")
    public ResponseEntity<Void> cliente(@RequestBody CadastroClienteRequest req) {
        cadastro.cadastrarCliente(req);
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    @PostMapping("/funcionario")
    public ResponseEntity<Void> funcionario(@RequestBody CadastroFuncionarioRequest req) {
        cadastro.cadastrarFuncionario(req);
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    @PostMapping("/ong")
    public ResponseEntity<Void> ong(@RequestBody CadastroOngRequest req) {
        cadastro.cadastrarOng(req);
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }
}
