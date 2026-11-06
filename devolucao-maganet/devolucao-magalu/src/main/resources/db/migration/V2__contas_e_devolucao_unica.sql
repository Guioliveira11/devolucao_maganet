-- Falha explicitamente se ja existirem duplicidades: reconciliar os dados antes de migrar.
-- Cada solicitacao devolve todo o ItemComprado (nao ha devolucao parcial nesta versao).
CREATE UNIQUE INDEX uq_devolucao_item_nao_cancelada
    ON solicitacoes_devolucao(item_comprado_id)
    WHERE status <> 'CANCELADA';

CREATE TABLE contas_acesso (
    id BIGSERIAL PRIMARY KEY,
    login VARCHAR(255) NOT NULL UNIQUE,
    senha_hash VARCHAR(255) NOT NULL,
    ativa BOOLEAN NOT NULL DEFAULT TRUE,
    usuario_id BIGINT UNIQUE REFERENCES usuarios(id),
    funcionario_id BIGINT UNIQUE REFERENCES funcionarios(id),
    CONSTRAINT ck_conta_um_vinculo CHECK (
        (usuario_id IS NOT NULL AND funcionario_id IS NULL) OR
        (usuario_id IS NULL AND funcionario_id IS NOT NULL)
    )
);
