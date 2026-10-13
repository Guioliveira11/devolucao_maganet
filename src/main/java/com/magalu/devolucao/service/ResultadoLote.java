package com.magalu.devolucao.service;
/** Selecionados inclui registros revalidados; processados conta alteracoes efetivas. */
public record ResultadoLote(int selecionados, int processados) {
    public static void validarTamanho(int tamanho) {
        if (tamanho < 1 || tamanho > 1000)
            throw new com.magalu.devolucao.exception.ParametroInvalidoException("Lote deve ter entre 1 e 1000 registros");
    }
}
