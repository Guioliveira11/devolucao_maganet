package com.magalu.devolucao.controller;

import com.magalu.devolucao.dto.DoacaoOngResponse;
import com.magalu.devolucao.dto.PaginaResponse;
import com.magalu.devolucao.service.DoacaoOngService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/ong/doacoes")
@RequiredArgsConstructor
public class OngController {

    private final DoacaoOngService doacoes;

    @GetMapping
    public PaginaResponse<DoacaoOngResponse> listar(@RequestParam(required = false) String status,
                                                    @RequestParam(defaultValue = "0") int pagina,
                                                    @RequestParam(defaultValue = "20") int tamanho) {
        return doacoes.listar(status, pagina, tamanho);
    }

    @PostMapping("/{id}/confirmar")
    public DoacaoOngResponse confirmar(@PathVariable Long id) {
        return doacoes.confirmarRecebimento(id);
    }
}
