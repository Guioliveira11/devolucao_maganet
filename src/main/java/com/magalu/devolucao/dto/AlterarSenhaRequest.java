package com.magalu.devolucao.dto;

/** Troca de senha pela conta logada. As senhas nunca aparecem em logs. */
public record AlterarSenhaRequest(String senhaAtual, String novaSenha) {
    @Override
    public String toString() {
        return "AlterarSenhaRequest[]";
    }
}
