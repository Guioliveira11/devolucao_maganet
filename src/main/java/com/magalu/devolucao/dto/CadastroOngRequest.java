package com.magalu.devolucao.dto;

import java.util.List;

/** Cadastro de ONG parceira: exige o codigo de convite configurado no ambiente. */
public record CadastroOngRequest(String nome, String cnpj, List<String> categorias, String codigoConvite,
                                 String login, String senha) {
    @Override
    public String toString() {
        return "CadastroOngRequest[login=" + login + "]";
    }
}
