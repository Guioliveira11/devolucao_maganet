package com.magalu.devolucao.controller;
import com.magalu.devolucao.dto.*;
import com.magalu.devolucao.service.NotificacaoService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
@RestController @RequestMapping("/api/v1/notificacoes") @RequiredArgsConstructor
public class NotificacaoController {
    private final NotificacaoService service;
    @GetMapping
    public PaginaResponse<NotificacaoResponse> listar(@RequestParam(defaultValue="false") boolean naoLidas,
            @RequestParam(defaultValue="0") int pagina,@RequestParam(defaultValue="20") int tamanho){
        return service.listar(naoLidas,pagina,tamanho);
    }
    @PostMapping("/{id}/ler")
    public ResponseEntity<Void> ler(@PathVariable long id){service.marcarLida(id);return ResponseEntity.noContent().build();}
    @GetMapping("/preferencias")
    public PreferenciasNotificacao preferencias(){return service.preferencias();}
    @PutMapping("/preferencias")
    public PreferenciasNotificacao preferencias(@Valid @RequestBody PreferenciasNotificacao p){return service.salvarPreferencias(p);}
}
