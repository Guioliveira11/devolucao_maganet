package com.magalu.devolucao.dto;
import java.util.List;
public record PaginaResponse<T>(List<T> itens, int pagina, int tamanho, long total) {
    public static void validar(int pagina, int tamanho) {
        if (pagina < 0 || tamanho < 1 || tamanho > 100)
            throw new com.magalu.devolucao.exception.ParametroInvalidoException("Pagina deve ser >= 0 e tamanho entre 1 e 100");
    }
}
