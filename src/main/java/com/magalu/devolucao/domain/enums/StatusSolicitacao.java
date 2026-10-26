package com.magalu.devolucao.domain.enums;

/**
 * Ciclo de vida da solicitacao de devolucao.
 * Usado pelo State Pattern implementado dentro de SolicitacaoDevolucao.
 */
public enum StatusSolicitacao {
    AGUARDANDO_ESCANEAMENTO,
    QR_EXPIRADO,
    EM_ANALISE,
    DESTINO_PENDENTE,
    CONCLUIDA,
    CANCELADA,
    REPROVADA
}
