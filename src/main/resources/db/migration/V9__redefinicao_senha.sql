-- 1) Toda conta passa a ter um e-mail (usado so para recuperar o acesso).
ALTER TABLE contas_acesso ADD COLUMN email VARCHAR(255);

-- Contas de cliente herdam o e-mail do cadastro do cliente.
UPDATE contas_acesso c
   SET email = lower(u.email)
  FROM usuarios u
 WHERE c.usuario_id = u.id;

CREATE UNIQUE INDEX ux_contas_acesso_email ON contas_acesso (lower(email)) WHERE email IS NOT NULL;

-- 2) Links de redefinicao. Guardamos apenas o HASH do token, nunca o token em si.
CREATE TABLE redefinicoes_senha (
    id BIGSERIAL PRIMARY KEY,
    conta_id BIGINT NOT NULL REFERENCES contas_acesso(id) ON DELETE CASCADE,
    token_hash VARCHAR(64) NOT NULL UNIQUE,
    criado_em TIMESTAMP NOT NULL,
    expira_em TIMESTAMP NOT NULL,
    usado_em TIMESTAMP
);

CREATE INDEX ix_redefinicoes_senha_conta ON redefinicoes_senha (conta_id, criado_em);

