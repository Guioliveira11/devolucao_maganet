-- E-mail para contas de funcionários. Registros existentes permanecem nulos até
-- que o endereço corporativo seja cadastrado/atualizado pelo administrador.
ALTER TABLE funcionarios ADD COLUMN email VARCHAR(255);
CREATE UNIQUE INDEX uq_funcionarios_email_lower
    ON funcionarios (lower(email)) WHERE email IS NOT NULL;

CREATE TABLE tokens_recuperacao_senha (
    id BIGSERIAL PRIMARY KEY,
    conta_acesso_id BIGINT NOT NULL REFERENCES contas_acesso(id) ON DELETE CASCADE,
    token_hash VARCHAR(64) NOT NULL UNIQUE,
    expira_em TIMESTAMP NOT NULL,
    utilizado_em TIMESTAMP
);
CREATE INDEX idx_tokens_recuperacao_conta ON tokens_recuperacao_senha(conta_acesso_id);
CREATE INDEX idx_tokens_recuperacao_expiracao ON tokens_recuperacao_senha(expira_em);
