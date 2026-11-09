package com.magalu.devolucao.dto;

/** Token e senha nova nunca aparecem em logs (toString mascarado). */
public record RedefinicaoConfirmarRequest(String token, String novaSenha) {
    @Override
    public String toString() {
        return "RedefinicaoConfirmarRequest[***]";
    }
}
