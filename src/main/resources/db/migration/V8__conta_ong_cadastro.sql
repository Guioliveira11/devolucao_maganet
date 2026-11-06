-- Terceiro tipo de acesso: contas de ONGs parceiras.
-- Cada conta continua ligada a exatamente UM perfil (cliente, funcionario ou ONG).
ALTER TABLE contas_acesso ADD COLUMN ong_id BIGINT UNIQUE REFERENCES ongs_parceiras(id);

ALTER TABLE contas_acesso DROP CONSTRAINT ck_conta_um_vinculo;
ALTER TABLE contas_acesso ADD CONSTRAINT ck_conta_um_vinculo CHECK (
    (usuario_id IS NOT NULL)::int + (funcionario_id IS NOT NULL)::int + (ong_id IS NOT NULL)::int = 1
);
