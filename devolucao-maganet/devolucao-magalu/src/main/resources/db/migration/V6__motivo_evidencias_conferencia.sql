-- Dados informados pelo cliente ao abrir a solicitacao.
-- Ficam opcionais no banco porque solicitacoes antigas nao tem essas informacoes.
ALTER TABLE solicitacoes_devolucao ADD COLUMN motivo VARCHAR(40);
ALTER TABLE solicitacoes_devolucao ADD COLUMN comentario VARCHAR(500);
ALTER TABLE solicitacoes_devolucao ADD COLUMN tipo_resolucao VARCHAR(20);

-- Resultado da conferencia feita pelo funcionario na loja.
ALTER TABLE solicitacoes_devolucao ADD COLUMN itens_conferidos VARCHAR(300);
ALTER TABLE solicitacoes_devolucao ADD COLUMN motivo_reprovacao VARCHAR(40);
ALTER TABLE solicitacoes_devolucao ADD COLUMN observacao_conferencia VARCHAR(500);
ALTER TABLE solicitacoes_devolucao ADD COLUMN data_conferencia TIMESTAMP;

-- Fotos enviadas pelo cliente. Sao comprimidas no celular antes do envio
-- (no maximo 3 por solicitacao), por isso cabem em uma coluna TEXT.
CREATE TABLE evidencias_devolucao (
    id BIGSERIAL PRIMARY KEY,
    solicitacao_id BIGINT NOT NULL REFERENCES solicitacoes_devolucao(id),
    tipo_conteudo VARCHAR(30) NOT NULL,
    conteudo_base64 TEXT NOT NULL,
    data_envio TIMESTAMP NOT NULL
);

CREATE INDEX idx_evidencias_solicitacao ON evidencias_devolucao(solicitacao_id);
