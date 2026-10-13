ALTER TABLE contas_acesso ADD COLUMN tentativas_falhas INT DEFAULT 0 NOT NULL;
ALTER TABLE contas_acesso ADD COLUMN bloqueado_ate TIMESTAMP;
