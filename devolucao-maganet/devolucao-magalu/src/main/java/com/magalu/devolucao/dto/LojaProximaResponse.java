package com.magalu.devolucao.dto;

import com.magalu.devolucao.domain.Loja;

public record LojaProximaResponse(
        Long id,
        String nome,
        String endereco,
        double distanciaKm
) {
    public static LojaProximaResponse from(Loja loja, double distanciaKm) {
        return new LojaProximaResponse(loja.getId(), loja.getNome(), loja.getEndereco(), distanciaKm);
    }
}
