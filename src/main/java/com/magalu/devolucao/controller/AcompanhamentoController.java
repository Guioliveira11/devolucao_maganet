package com.magalu.devolucao.controller;
import com.magalu.devolucao.dto.*;
import com.magalu.devolucao.domain.enums.*;
import com.magalu.devolucao.service.AcompanhamentoService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
@RestController @RequiredArgsConstructor
public class AcompanhamentoController {
    private final AcompanhamentoService service;
    @GetMapping("/api/v1/devolucoes")
    public PaginaResponse<AcompanhamentoResponse> listar(@RequestParam(required=false) StatusSolicitacao status,
            @RequestParam(defaultValue="0") int pagina,@RequestParam(defaultValue="20") int tamanho){
        return service.listar(status,pagina,tamanho);
    }
    @GetMapping("/api/v1/devolucoes/{id}/acompanhamento")
    public AcompanhamentoResponse detalhe(@PathVariable long id){return service.detalhe(id);}
    @GetMapping("/api/v1/devolucoes/{id}/historico")
    public PaginaResponse<EventoClienteResponse> historico(@PathVariable long id,
            @RequestParam(defaultValue="0") int pagina,@RequestParam(defaultValue="20") int tamanho){
        return service.historico(id,pagina,tamanho);
    }
    @GetMapping("/api/v1/funcionario/devolucoes")
    public PaginaResponse<FilaLojaResponse> fila(@RequestParam(required=false) StatusSolicitacao status,
            @RequestParam(required=false) DestinoProduto destino,@RequestParam(required=false) Long minimoHoras,
            @RequestParam(defaultValue="0") int pagina,@RequestParam(defaultValue="20") int tamanho){
        return service.fila(status,destino,minimoHoras,pagina,tamanho);
    }
}
