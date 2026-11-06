package com.magalu.devolucao.dto;

/** Cadastro publico de cliente. A senha nunca aparece em logs (toString mascarado). */
public record CadastroClienteRequest(String nome, String cpf, String email, String telefone,
                                     String login, String senha) {
    @Override
    public String toString() {
        return "CadastroClienteRequest[login=" + login + "]";
    }
}
