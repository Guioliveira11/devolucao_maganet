ALTER TABLE solicitacoes_devolucao ADD COLUMN data_recebimento TIMESTAMP;
-- Backfill apenas quando existe evidencia de recebimento no historico.
UPDATE solicitacoes_devolucao s SET data_recebimento=a.recebida
FROM (SELECT solicitacao_id,min(timestamp) AS recebida FROM registros_auditoria
      WHERE acao='QR_ESCANEADO' GROUP BY solicitacao_id) a
WHERE s.id=a.solicitacao_id;
CREATE INDEX idx_solicitacoes_cliente_data ON solicitacoes_devolucao(usuario_id,data_criacao DESC,id DESC);
CREATE INDEX idx_solicitacoes_fila ON solicitacoes_devolucao(loja_id,data_recebimento,id)
    WHERE status IN ('EM_ANALISE','DESTINO_PENDENTE');
CREATE INDEX idx_auditoria_solicitacao_data ON registros_auditoria(solicitacao_id,timestamp DESC,id DESC);
CREATE TABLE preferencias_notificacao (
    usuario_id BIGINT PRIMARY KEY REFERENCES usuarios(id),
    atualizacoes BOOLEAN NOT NULL DEFAULT TRUE,
    lembretes_qr BOOLEAN NOT NULL DEFAULT TRUE
);
CREATE TABLE notificacoes (
    id BIGSERIAL PRIMARY KEY,
    usuario_id BIGINT NOT NULL REFERENCES usuarios(id),
    solicitacao_id BIGINT NOT NULL REFERENCES solicitacoes_devolucao(id),
    chave VARCHAR(120) NOT NULL UNIQUE,
    tipo VARCHAR(40) NOT NULL,
    mensagem TEXT NOT NULL,
    criada_em TIMESTAMP NOT NULL,
    lida_em TIMESTAMP,
    qr_expira_em TIMESTAMP
);
CREATE INDEX idx_notificacoes_cliente_data ON notificacoes(usuario_id,criada_em DESC,id DESC);
CREATE INDEX idx_notificacoes_nao_lidas ON notificacoes(usuario_id,id) WHERE lida_em IS NULL;
CREATE INDEX idx_qrcodes_lembretes ON qrcodes_devolucao(data_expiracao,solicitacao_id) WHERE status='ATIVO';
CREATE INDEX idx_qrcodes_solicitacao ON qrcodes_devolucao(solicitacao_id);
