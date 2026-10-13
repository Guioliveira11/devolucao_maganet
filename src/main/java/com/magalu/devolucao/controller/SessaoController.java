package com.magalu.devolucao.controller;
import com.magalu.devolucao.dto.SessaoResponse;
import com.magalu.devolucao.service.IdentidadeService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController @RequiredArgsConstructor
public class SessaoController {
    private final IdentidadeService identidade;
    @GetMapping("/api/v1/sessao")
    public SessaoResponse sessao() { return identidade.sessao(); }
}
