package com.magalu.devolucao.controller;

import com.magalu.devolucao.dto.LojaProximaResponse;
import com.magalu.devolucao.service.LojaProximaService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/lojas")
@RequiredArgsConstructor
public class LojaController {


    private final LojaProximaService lojaProximaService;
    private final com.magalu.devolucao.service.LojaConsultaService consulta;

    @GetMapping
    public com.magalu.devolucao.dto.PaginaResponse<com.magalu.devolucao.dto.LojaResponse> listar(
            @RequestParam(defaultValue="") String busca, @RequestParam(defaultValue="0") int pagina,
            @RequestParam(defaultValue="20") int tamanho) {
        return consulta.listar(busca,pagina,tamanho);
    }

    @GetMapping("/proximas")
    public List<LojaProximaResponse> lojasProximas(
            @RequestParam double latitude,
            @RequestParam double longitude,
            @RequestParam(required = false, defaultValue = "20.0") double raioKm) {

        return lojaProximaService.buscarLojasProximas(latitude, longitude, raioKm).stream()
                .map(loja -> LojaProximaResponse.from(loja, loja.calcularDistancia(latitude, longitude)))
                .toList();
    }
}
