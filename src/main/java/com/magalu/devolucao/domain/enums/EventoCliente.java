package com.magalu.devolucao.domain.enums;
// Mensagens publicas controladas: nunca encaminhar autor ou detalhes da auditoria.
public enum EventoCliente {
    SOLICITACAO_CRIADA("Sua solicitacao de devolucao foi criada."),
    QR_RENOVADO("O QR da sua devolucao foi renovado."),
    SOLICITACAO_CANCELADA("Sua solicitacao foi cancelada."),
    QR_ESCANEADO("O produto foi recebido na loja e esta em analise."),
    DESTINO_DEFINIDO("O destino do produto foi definido."),
    QR_EXPIRADO("O QR expirou. Consulte sua devolucao para verificar a possibilidade de renovacao."),
    CONFERENCIA_REPROVADA("A devolucao nao foi aprovada na conferencia da loja. Veja o motivo no acompanhamento.");
    private final String mensagem;
    EventoCliente(String mensagem) { this.mensagem = mensagem; }
    public String mensagem() { return mensagem; }
}
