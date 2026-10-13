package com.magalu.devolucao.service;
import com.magalu.devolucao.dto.*;
import com.magalu.devolucao.repository.LojaRepository;
import com.magalu.devolucao.exception.ParametroInvalidoException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.data.domain.*;

@Service @RequiredArgsConstructor
public class LojaConsultaService {
    private final LojaRepository lojas;
    @Transactional(readOnly=true)
    public PaginaResponse<LojaResponse> listar(String busca, int pagina, int tamanho) {
        PaginaResponse.validar(pagina,tamanho);
        String termo=busca==null?"":busca.trim();
        if(termo.length()>120) throw new ParametroInvalidoException("Busca deve ter ate 120 caracteres");
        var resultado=lojas.findByAtivaTrueAndNomeContainingIgnoreCaseOrAtivaTrueAndEnderecoContainingIgnoreCase(
                termo,termo,PageRequest.of(pagina,tamanho,Sort.by("nome","id")));
        return new PaginaResponse<>(resultado.getContent().stream()
                .map(l->new LojaResponse(l.getId(),l.getNome(),l.getEndereco(),l.getHorarioFuncionamento())).toList(),
                pagina,tamanho,resultado.getTotalElements());
    }
}
