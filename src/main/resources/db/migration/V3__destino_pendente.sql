ALTER TABLE solicitacoes_devolucao ADD COLUMN ong_destinataria_id BIGINT REFERENCES ongs_parceiras(id);
ALTER TABLE solicitacoes_devolucao ADD COLUMN data_definicao_destino TIMESTAMP;
CREATE INDEX idx_solicitacoes_ong ON solicitacoes_devolucao(ong_destinataria_id);
CREATE INDEX idx_pedidos_usuario ON pedidos(usuario_id);
-- Historico CONCLUIDA e preservado: reconciliar externamente se necessario.
