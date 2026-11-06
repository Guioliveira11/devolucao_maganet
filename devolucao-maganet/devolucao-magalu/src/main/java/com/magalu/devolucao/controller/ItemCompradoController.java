package com.magalu.devolucao.controller;

import com.magalu.devolucao.dto.ItemCompradoResponse;
import com.magalu.devolucao.service.DevolucaoService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController @RequestMapping("/api/v1/itens") @RequiredArgsConstructor
public class ItemCompradoController {
    private final DevolucaoService devolucaoService;
    @GetMapping
    public com.magalu.devolucao.dto.PaginaResponse<ItemCompradoResponse> listar(
            @RequestParam(defaultValue="0") int pagina, @RequestParam(defaultValue="20") int tamanho) {
        return devolucaoService.listarItens(pagina, tamanho);
    }
}
