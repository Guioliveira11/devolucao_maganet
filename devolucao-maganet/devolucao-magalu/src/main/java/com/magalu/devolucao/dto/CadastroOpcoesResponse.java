package com.magalu.devolucao.dto;

import java.util.List;

/** O que a tela de cadastro pode oferecer neste ambiente. */
public record CadastroOpcoesResponse(boolean cadastroFuncionario, boolean cadastroOng,
                                     List<LojaOpcao> lojas, List<String> categorias) {
    public record LojaOpcao(Long id, String nome) {}
}
