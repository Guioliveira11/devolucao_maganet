package com.magalu.devolucao.dto;

/** Cadastro de funcionario: exige o codigo de convite configurado no ambiente. */
public record CadastroFuncionarioRequest(String nome, String matricula, Long lojaId, String codigoConvite,
                                         String login, String senha) {
    @Override
    public String toString() {
        return "CadastroFuncionarioRequest[login=" + login + "]";
    }
}
