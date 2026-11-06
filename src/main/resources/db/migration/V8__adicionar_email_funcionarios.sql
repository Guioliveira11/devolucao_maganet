ALTER TABLE funcionarios
ADD COLUMN email VARCHAR(255);

ALTER TABLE funcionarios
ADD CONSTRAINT uk_funcionarios_email UNIQUE (email);